package com.aopro.wordlink.controller

import com.aopro.wordlink.AuthorizationException
import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.*
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.aopro.wordlink.utilities.generateRandomSHA512
import com.google.gson.annotations.Expose
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.time.LocalDateTime
import java.util.*

object Reviews {
    private val reviews = mutableListOf<Review>()
    private lateinit var session: MongoCollection<Review.Model>

    fun reviews() = reviews.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Review.Model>("reviews")

        reviews.addAll(session.find().map { model ->
            Review(
                id = model._id,
                name = model.name,
                description = model.description,
                owner = Users.users().find { usr -> usr.id == model._id } ?: User.notExistObject(),
                entries = model.entries
                    .map { ent -> Words.words().find { word -> word.id == ent } ?: Word.notExistObject() }
                    .toMutableList(),
                answers = model.answers
                    .map { ent ->
                        Answers.answers().find { answer -> answer.word.id == ent } ?: Answer.notExistObject()
                    }
                    .toMutableList(),
                finished = model.finished,
                createdAt = Date(model.createdAt * 1000),
                updatedAt = Date(model.updatedAt * 1000)
            )
        })
    }

    /** 重複のないIDを生成します。*/
    tailrec fun generateNoDuplicationId(): String {
        val length = 8
        var builder = ""
        val elements = ensureIdElemments.toMutableList()
        for (i in 0..length) {
            builder += elements.random()
        }

        return if (Reviews.reviews.filter { category -> category.id == builder }.isEmpty()) builder else generateNoDuplicationId()
    }

    /** データベースにデータを追加*/
    fun insertReview(review: Review) {
        session.insertOne(Review.Model(
            _id = review.id,
            name = review.name,
            description = review.description,
            entries = review.entries.map { entry -> entry.id } as MutableList<String>,
            answers = review.answers.map { answer -> answer.id } as MutableList<String>,
            finished = review.finished,
            updatedAt = review.updatedAt.time,
            createdAt = review.createdAt.time

        ))
        reviews.add(review)
    }

    /** データベースを更新 */
    fun updateReview(vararg reviewes: Review) {
        reviewes.forEach { review ->
            session.updateOne(
                Review.Model::_id eq review.id,
                Review.Model::name setTo review.name,
                Review.Model::description setTo review.description,
                Review.Model::entries setTo review.entries.map { entry -> entry.id } as MutableList<String>,
                Review.Model::answers setTo review.answers.map { answer -> answer.id } as MutableList<String>,
                Review::finished setTo review.finished,
                Review.Model::updatedAt setTo Date
                    .from(
                        LocalDateTime
                            .now()
                            .atZone(DefaultZone)
                            .toInstant()
                    ).time)
        }
    }
}

object Markers {
    val markers = mutableListOf<Marker>()
}

@Location("/review")
class ReviewRoute {

    @Location("/create")
    class Create {
        data class Payload(
            val category: String = "",
            val from: Int = 0,
            val end: Int = 0,
            val shuffled: Boolean = false
        )
    }

    @Location("/:target")
    class View {

        /** CSRF防止の為、回答専用のセッションを設ける */
        @Location("/let/")
        class Let {

            data class Question(
                @Expose val name: String = "",
                @Expose val mean: String = "",
                @Expose val id: String = "",
                @Expose val representCorrect: String = "",
                @Expose val representIncorrect: String = ""
            )

            @Location("/mark/:id")
            data class Mark(val id: String = "") {
                data class Payload(
                    val result: String = "",
                    val target: String = ""
                )
            }
        }

    }


}

fun Route.reviews() {

    post<ReviewRoute.Create> {
        val authUser = context.request.tokenAuthentication()
        val payload = context.receive(ReviewRoute.Create.Payload::class)
        val target = Categories.categories().find { category -> category.id == payload.category } ?: throw BadRequestException("指定されたカテゴリーが見つかりせん")
        val entries = Words.words()
            .filter { word -> word.category.id == target.id }
            .sortedBy { word -> word.number }
            .subList(payload.from - 1, payload.end)

        val review = Review(
            id = Reviews.generateNoDuplicationId(),
            name = "Test",
            description = "",
            owner = authUser,
            entries = entries.toMutableList(),
            answers = mutableListOf(),
            finished = false,
            createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
            updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
        )

        Reviews.insertReview(review) //DBに追加
        context.respond(ResponseInfo(
            data = review
        ))
    }

    /** 指定されたユーザーの回答結果を取得する。ただし、他のユーザーのデータを取得する場合は、アクセスレベル２以上が必要。*/
    get<ReviewRoute.View> { query ->
        val authUser = context.request.tokenAuthentication()
        val targetId: String = context.parameters["target"]!!
        val targetUser =
            Users.users().find { user -> targetId == user.id } ?: throw BadRequestException("指定されたユーザーが見つかりません")

        if (targetUser.id != authUser.id && authUser.accessLevel < 2) throw AuthorizationException("権限が足りません")

        context.respond(ResponseInfo(
            data = Reviews.reviews().filter { review -> review.owner.id == targetUser.id }
        ))

    }

    post<ReviewRoute.View.Let> {
        val authUser = context.request.tokenAuthentication()
        val targetId = context.parameters["target"]
        val target = Reviews.reviews().find { review -> review.id == targetId }
            ?: throw BadRequestException("Not correct review_id")

        val marker = Marker(
            id = generateRandomSHA512,
            correctsCheck = generateRandomSHA512,
            incorrectCheck = generateRandomSHA512,
            reflectReview = target,
            createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
            updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
        )
        Markers.markers.add(marker)

        val startIndex = target.answers.size
        val next = target.entries.get(startIndex)

        context.respond(
            ResponseInfo(
                data = ReviewRoute.View.Let.Question(
                    id = next.id,
                    name = next.id,
                    mean = next.mean,
                    representCorrect = marker.correctsCheck,
                    representIncorrect = marker.incorrectCheck
                )
            )
        )
    }


    post<ReviewRoute.View.Let.Mark> {
        val authUser = context.request.tokenAuthentication()
        val payload = context.receive(ReviewRoute.View.Let.Mark.Payload::class)
        val targetId = context.parameters["target"]
        val id = context.parameters["target"]
        val target = Reviews.reviews().find { review -> review.id == targetId }
            ?: throw BadRequestException("Not correct review_id")
        val marker = Markers.markers.find { marker -> marker.id == id }
            ?: throw BadRequestException("Not correct marker_id")

        if (marker.reflectReview.id != target.id) throw BadRequestException("Not correct marker")
        val isCorrect = if (payload.result == marker.correctsCheck) true else false

        val targetWord = Words.words().find { word -> word.id == payload.target } ?: throw BadRequestException("Not correct word_target")
        val answer = authUser.getAnswer(targetWord)
        target.answers.add(answer.apply {
            histories.add(
                Answer.History(
                    impactReview = target,
                    result = if (isCorrect) 1 else 0,
                    postAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
                )
            )
        })

        Reviews.updateReview(target)
        Answers.updateAnswer(answer)

        //次の問題を出題
        if (target.answers.size < target.entries.size) {
            val startIndex = target.answers.size
            val next = target.entries.get(startIndex)

            context.respond(
                ResponseInfo(
                    data = ReviewRoute.View.Let.Question(
                        id = next.id,
                        name = next.id,
                        mean = next.mean
                    )
                )
            )

        } else {
            context.respond(
                ResponseInfo(
                   data = "finished"
                )
            )
        }
    }
}
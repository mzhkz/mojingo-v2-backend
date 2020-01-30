package com.aopro.wordlink.controller

import com.aopro.wordlink.AuthorizationException
import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.*
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route
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

        reviews.addAll(session.find().map{ model ->
            Review(
                id = model._id,
                name = model.name,
                description = model.description,
                owner = Users.users().find { usr -> usr.id == model._id } ?: User.notExistObject(),
                entries = model.entries
                    .map { ent -> Words.words().find { word ->  word.id == ent} ?: Word.notExistObject() }
                    .toMutableList(),
                answers = model.answers
                    .map { ent -> Answers.answers().find { answer ->  answer.word.id == ent} ?: Answer.notExistObject() }
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
                            .toInstant()).time)
        }
    }
}

object Markers {

    private val markers: MutableList<Marker> = mutableListOf()

    fun markers() = markers.toMutableList()

    fun initialize() {

    }

    /** マーカーを登録する。*/
    fun registerMarker(marker: Marker) {
        markers.add(marker)
    }

    /** UUIDで運用してみる。 */
    fun generateSecureId(): String {
        return UUID.randomUUID().toString().replace('-', '.')
    }

}


@Location("/review")
class ReviewRoute {

    @Location("/:target")
    data class View(val target: String) {

        /** CSRF防止の為、回答専用のセッションを設ける */
        @Location("/let")
        class Let {

            /** 回答を記録する。レビューセッションを含めなければいけない */
            @Location("/mark")
            class Mark {

                data class Payload(
                    val result: Int,
                    val session: String
                )
            }
        }
    }


}

fun Route.reviews() {

    /** 指定されたユーザーの回答結果を取得する。ただし、他のユーザーのデータを取得する場合は、アクセスレベル２以上が必要。*/
    get <ReviewRoute.View>{ query ->
        val targetUser = Users.users().find { user -> query.target == user.id } ?: throw BadRequestException("指定されたユーザーが見つかりません")
        val authUser = context.request.tokenAuthentication()

        if (targetUser.id != authUser.id && authUser.accessLevel < 2) throw AuthorizationException("権限が足りません")

        context.respond(ResponseInfo(
            data = Reviews.reviews().filter { review -> review.owner.id == targetUser.id }
        ))

    }

    post<ReviewRoute.View.Let> {

    }


    post<ReviewRoute.View.Let.Mark> {


    }

}
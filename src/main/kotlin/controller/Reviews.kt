package me.mojingo.v2.backend.controller

import me.mojingo.v2.backend.AuthorizationException
import me.mojingo.v2.backend.BadRequestException
import me.mojingo.v2.backend.ResponseInfo
import me.mojingo.v2.backend.database.DatabaseHandler
import me.mojingo.v2.backend.database.model.*
import me.mojingo.v2.backend.requireNotNullAndNotEmpty
import me.mojingo.v2.backend.utilities.*
import com.google.gson.annotations.Expose
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object Reviews {
    private val reviews = mutableListOf<Review>()
    private lateinit var session: MongoCollection<Review.Model>

    fun reviews() = reviews.toMutableList()

    /** Sheetを更新する */
    fun replace(user: User) {
        reviews.removeAll { review -> review.owner == user }
        reviews.addAll(session.find(Review.Model::owner_id eq user.id).map { model ->
            adapt(model)
        })
    }

    private fun adapt(model: Review.Model) =
        Review(
            id = model._id,
            name = model.name,
            description = model.description,
            owner = Users.users().find { usr -> usr.id == model.owner_id } ?: User.notExistObject(),
            entries = model.entries
                .map { ent ->
                    Words.words().find { word -> word.id == ent } ?: Word.notExistObject() }
                .toMutableList(),
            answers = model.answers
                .map { ent ->
                    Answers.answers().find { answer -> answer.id == ent } ?: Answer.notExistObject()
                }.toMutableList(),
            finished = model.finished,
            createdAt = model.createdAt,
            updatedAt = model.createdAt
        )

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Review.Model>("reviews")

        reviews.addAll(session.find().map { model ->
            adapt(model)
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
            owner_id = review.owner.id,
            finished = review.finished,
            updatedAt = review.updatedAt,
            createdAt = review.createdAt

        ))
        reviews.add(review)
    }

    /** データベースを更新 */
    fun updateReview(review: Review) {
        session.updateOne(
            Review.Model::_id eq review.id,
            Review.Model::name setTo review.name,
            Review.Model::description setTo review.description,
            Review.Model::entries setTo review.entries.map { entry -> entry.id } as MutableList<String>,
            Review.Model::answers setTo review.answers.map { answer -> answer.id } as MutableList<String>,
            Review.Model::finished setTo review.finished,
            Review.Model::owner_id setTo review.owner.id,
            Review.Model::updatedAt setTo CurrentUnixTime
        )
    }


    fun deleteReview(review: Review) {
        session.deleteOne(Review.Model::_id eq review.id)
        reviews.removeIf { rev -> rev.id == review.id }
    }
}

object Markers {
    val markers = mutableListOf<Marker>()
}

@Location("/reviews")
class ReviewRoute {

    @Location("/create")
    class Create {
        data class Payload(
            @Expose val category: String = "",
            @Expose val start: Int = 0,
            @Expose val end: Int = 0,
            @Expose val shuffle: Boolean = false
        )
    }

    @Location("{target}")
    data class List(val target: String = "") {


        data class ListResponse(
            @Expose val reviews: MutableList<ReviewResponse> = mutableListOf(),
            @Expose val pageSize: Int = 0
        )

        data class ReviewResponse(
            @Expose val review: Review? = null,
            @Expose val correctSize: Int = 0,
            @Expose val incorrectSize: Int = 0,
            @Expose val percentage: Int = 0,
            @Expose val createAgo: String = ""
        )
        @Location("{id}")
        data class View(val id: String = "") {

            @Location("/finished")
            class Finished

            @Location("/qrcode")
            class QRCode

            @Location("/delete")
            class Delete

            /** CSRF防止の為、回答専用のセッションを設ける */
            @Location("/let")
            class Let {

                data class Question(
                    @Expose val id: String = "",
                    @Expose val name: String = "",
                    @Expose val mean: String = "",
                    @Expose val description: String = "",
                    @Expose val categoryId: String = "",
                    @Expose val wordId: String = "",
                    @Expose val owner: User? = User.notExistObject(),
                    @Expose val representCorrect: String = "",
                    @Expose val representIncorrect: String = "",
                    @Expose val number: Int = 0
                )

                @Location("/mark/{marker}")
                data class Mark(val marker: String = "") {
                    data class Payload(
                        @Expose val result: String = "",
                        @Expose val target: String = ""
                    )
                }
            }
        }

    }


}

fun Route.reviews() {

    post<ReviewRoute.Create> {
        val authUser = context.request.tokenAuthentication()
        val payload = context.receive(ReviewRoute.Create.Payload::class)
        requireNotNullAndNotEmpty(payload.category, payload.start, payload.end, payload.shuffle)

        val target = Categories.categories().find { category -> category.id == payload.category } ?: throw BadRequestException("指定されたカテゴリーが見つかりせん")
        val candidate = Words.words()
            .filter { word -> word.category.id == target.id }

        val min = candidate.minBy { word -> word.number }!!.number
        val max = candidate.maxBy { word -> word.number }!!.number

        if (!(payload.start in min..max && payload.end in (payload.start + 1)..max)) throw BadRequestException("指定された範囲が無効です ${payload.start} ~ ${payload.end}")

        val entries = Words.words()
            .filter { word -> word.category.id == target.id && word.number in payload.start..payload.end}
            .shuffled()

        val review = Review(
            id = Reviews.generateNoDuplicationId(),
            name = "${target.name} ${payload.start} ~ ${payload.end}",
            description = "",
            owner = authUser,
            entries = entries.toMutableList(),
            answers = mutableListOf(),
            finished = false,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )

        Reviews.insertReview(review) //DBに追加
        context.respond(ResponseInfo(
            data = review
        ))
    }

    /** 指定されたユーザーの回答結果を取得する。ただし、他のユーザーのデータを取得する場合は、アクセスレベル２以上が必要。*/
    get<ReviewRoute.List> { query ->
        val authUser = context.request.tokenAuthentication()

        val pageNumber: Int = context.request.queryParameters["page"]?.toInt() ?: 1
        val userId: String = context.parameters["target"]!!
        val targetUser =
            if (userId == "me" || userId == authUser.id)
                authUser
            else {
                if (authUser.accessLevel >= 2)
                    Users.users().find { user -> user.id == userId } ?: throw BadRequestException("Not found '$userId' as User.")
                else throw AuthorizationException("Your token doesn't access this content.")
            }

        if (targetUser.id != authUser.id && authUser.accessLevel < 2) throw AuthorizationException("権限が足りません")

        val entire = Reviews.reviews()
            .filter { review -> review.owner.id == targetUser.id }
        val target = entire.reversed()
            .splitAsPagination(page = pageNumber, index = 10)
            .map { review ->
                val impacts = Answers.answers().mapNotNull { answer -> answer.histories.find { history ->  history.impactReviewId == review.id}}
                val correctSize = impacts.count { history -> history.result == 1 }
                val incorrectSize = impacts.count { history -> history.result == 0 }

                ReviewRoute.List.ReviewResponse(
                    review = review,
                    correctSize = correctSize,
                    incorrectSize = incorrectSize,
                    percentage = Math.floor(correctSize.toDouble() /( correctSize + incorrectSize).toDouble() * 100.0).toInt(),
                    createAgo = review.createdAt.currentUnixTimediff()
                )
            }

        context.respond(ResponseInfo(
            data = ReviewRoute.List.ListResponse(
                reviews = target.toMutableList(),
                pageSize = entire.maximumAsPagination(10)
            )
        ))
    }

    get<ReviewRoute.List.View> {
        val authUser = context.request.tokenAuthentication()
        val reviewId: String = context.parameters["id"]!!
        val userId: String = context.parameters["target"]!!
        val targetUser =
            if (userId == "me" || userId == authUser.id)
                authUser
            else {
                if (authUser.accessLevel >= 2)
                    Users.users().find { user -> user.id == userId } ?: throw BadRequestException("Not found '$userId' as User.")
                else throw AuthorizationException("アクセス権限がありません")
            }

        val target = Reviews.reviews().find { review -> review.id ==  reviewId && targetUser.id == review.owner.id}
            ?: throw BadRequestException("Not found $reviewId.")

        val impacts = Answers.answers().mapNotNull { answer -> answer.histories.find { history ->  history.impactReviewId == target.id}}
        val correctSize = impacts.count { history -> history.result == 1 }
        val incorrectSize = impacts.count { history -> history.result == 0 }

        context.respond(ResponseInfo(data = ReviewRoute.List.ReviewResponse(
            review = target,
            correctSize = impacts.count { history -> history.result == 1 } ,
            incorrectSize = impacts.count { history -> history.result == 0 },
            percentage = Math.floor(correctSize.toDouble() /( correctSize + incorrectSize).toDouble() * 100.0).toInt(),
            createAgo = target.createdAt.currentUnixTimediff()
        )))
    }

    get<ReviewRoute.List.View.QRCode> {
        val reviewId: String = context.parameters["id"]!!
        val userId: String = context.parameters["target"]!!
        val appDomain: String = context.request.queryParameters["appDomain"]!!
        val target = Reviews.reviews().find { review -> review.id ==  reviewId }
            ?: throw BadRequestException("Not found $reviewId.")

        val contents = "${appDomain}/reviews/${userId}/${target.id}/marking?onlyRecord=true"
        val format = BarcodeFormat.QR_CODE
        val size = 100

        val hints = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)

        val writter = QRCodeWriter()
        val bitMatrix = writter.encode(contents, format, size, size, hints)
        val image = MatrixToImageWriter.toBufferedImage(bitMatrix)
        val imageFile = File("./temperature/barcode.png")
        ImageIO.write(image, "png",imageFile)

        context.respondFile(imageFile)
    }

    post<ReviewRoute.List.View.Delete> {
        val authUser = context.request.tokenAuthentication()
        val reviewId = context.parameters["id"]

        val target = Reviews.reviews().find { review -> review.id == reviewId && authUser.id == review.owner.id}
            ?: throw AuthorizationException("この操作は、小テスト作成者の本人のみが行なえます。")

        Reviews.deleteReview(target)

        context.respond(ResponseInfo(message = "has been succeed"))
    }


    post<ReviewRoute.List.View.Finished> {
        val authUser = context.request.tokenAuthentication()
        val reviewId = context.parameters["id"]

        val target = Reviews.reviews().find { review -> review.id == reviewId && authUser.id == review.owner.id}
            ?: throw AuthorizationException("この操作は、小テスト作成者の本人のみが行なえます。")

        Reviews.updateReview(target.apply {
            finished = true
        })

        context.respond(ResponseInfo(message = "has been succeed"))
    }

    post<ReviewRoute.List.View.Let> {
        val authUser = context.request.tokenAuthentication()
        val reviewId = context.parameters["id"]

        val target = Reviews.reviews().find { review -> review.id == reviewId}
            ?: throw BadRequestException("Not correct review_id")
        if (target.owner.id != authUser.id && authUser.accessLevel < 2) throw AuthorizationException("アクセス権限がありません")

        val marker = Marker(
            id = generateRandomSHA512,
            correctsCheck = generateRandomSHA512,
            incorrectCheck = generateRandomSHA512,
            reflectReview = target,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )
        Markers.markers.add(marker)

        val startIndex = target.answers.size
        val entrySize = target.entries.size

        if (startIndex >= entrySize) throw BadRequestException("全て解き終わっています！全${entrySize}問")
        val next = target.entries.get(startIndex)

        context.respond(
            ResponseInfo(
                data = ReviewRoute.List.View.Let.Question(
                    id = marker.id,
                    wordId = next.id,
                    categoryId = next.category.id,
                    name = next.name,
                    mean = next.mean,
                    description = next.description,
                    owner = target.owner,
                    representCorrect = marker.correctsCheck,
                    representIncorrect = marker.incorrectCheck,
                    number = startIndex + 1
                )
            )
        )
    }


    post<ReviewRoute.List.View.Let.Mark> {
        val authUser = context.request.tokenAuthentication()

        val payload = context.receive(ReviewRoute.List.View.Let.Mark.Payload::class)
        val reviewId = context.parameters["id"]
        val markerId = context.parameters["marker"]

        requireNotNullAndNotEmpty(payload.result, payload.target)

        val target = Reviews.reviews().find { review -> review.id == reviewId }
            ?: throw BadRequestException("Not correct review_id")
        if (target.owner.id != authUser.id && authUser.accessLevel < 2) throw AuthorizationException("アクセス権限がありません")
        val marker = Markers.markers.find { marker -> marker.id == markerId }
            ?: throw BadRequestException("回答専用セッションが無効です。リロードしてください")

        if (marker.reflectReview.id != target.id) throw BadRequestException("回答専用セッションが無効です。")
        val isCorrect = payload.result == marker.correctsCheck

        val targetWord = Words.words().find { word -> payload.target == word.id }
            ?: throw BadRequestException("Not correct word_target ${payload.target}")

        val answer = target.owner.getAnswer(targetWord)

        if (answer.histories.find { history -> history.impactReviewId == target.id } != null)
            throw BadRequestException("この問題はすでに回答しています。${answer.word.name}")

        if (Answers.isExamWordWithAnswer(answer)) { //ランク変動の時期だった場合
            answer.rank += if (isCorrect) 1 else -1 //正解の場合は+1, 間違えた場合は-1

            if (answer.rank < 0)
                answer.rank = 0
        }
        target.owner.refreshLastAnswered(targetWord.category, answer)

        target.answers.add(answer.apply {
            histories.add(
                Answer.History(
                    impactReviewId = target.id,
                    result = if (isCorrect) 1 else 0,
                    postAt = CurrentUnixTime
                )
            )
        })

        Reviews.updateReview(target)
        Answers.updateAnswer(answer)

        //次の問題を出題
        if (target.answers.size < target.entries.size) {

            //更新
            marker.apply {
                correctsCheck = generateRandomSHA512
                incorrectCheck = generateRandomSHA512
            }

            val startIndex = target.answers.size
            val next = target.entries.get(startIndex)

            context.respond(
                ResponseInfo(
                    data = ReviewRoute.List.View.Let.Question(
                        id = marker.id,
                        wordId = next.id,
                        name = next.name,
                        mean = next.mean,
                        description = next.description,
                        categoryId = next.category.id,
                        owner = null,
                        representCorrect = marker.correctsCheck,
                        representIncorrect = marker.incorrectCheck,
                        number = startIndex + 1
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
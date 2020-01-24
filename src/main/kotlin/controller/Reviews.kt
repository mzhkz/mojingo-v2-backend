package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Review
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.routing.Route
import io.ktor.routing.get
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
                entries = model.entries
                    .map { ent -> Words.words().find { word ->  word.id == ent} ?: Word.notExistObject() }
                    .toMutableList(),
                answers = mutableListOf(),
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
                Review.Model::updatedAt setTo Date
                    .from(
                        LocalDateTime
                            .now()
                            .atZone(DefaultZone)
                            .toInstant()).time)
        }
    }
}

@Location("/review")
class ReviewRoute {

    @Location("/:target")
    data class View(val target: String)
}

fun Route.reviews() {

}
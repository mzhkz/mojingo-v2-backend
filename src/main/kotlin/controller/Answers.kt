package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.*
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.routing.Route
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.time.LocalDateTime
import java.util.*

object Answers {

    private val answers = mutableListOf<Answer>()
    private lateinit var session: MongoCollection<Answer.Model>

    fun answers() = answers.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Answer.Model>("answers")

        answers.addAll(session.find().map { model->
            Answer(
                id = model._id,
                user = Users.users().find { user -> user.id == model.userId } ?: User.notExistObject(),
                word = Words.words().find { word -> word.id == model.wordId } ?: Word.notExistObject(),
                createdAt = Date(model.created_at * 1000),
                updatedAt = Date(model.updated_at * 1000),
                histories = model.histories
                    .map { historyModel ->
                        Answer.History(
                            impactReview = Reviews
                                .reviews()
                                .find { review -> review.id == historyModel.impact_review } ?: Review.notExistObject(),
                            result = historyModel.result,
                            postAt = Date(historyModel.post_at * 1000)
                        )
                    } as MutableList<Answer.History>
            )
        })
    }

    /** 重複のないIDを生成します*/
    tailrec fun generateNoDuplicationId(): String {
        val length = 8
        var builder = ""
        val elements = ensureIdElemments.toMutableList()
        for (i in 0..length) {
            builder += elements.random()
        }
        return if (answers.filter { answer -> answer.id == builder }.isEmpty()) builder else generateNoDuplicationId()
    }

    /** 回答をデータベースに記録*/
    fun insertAnswer(answer: Answer) {
        session.insertOne(Answer.Model(
            _id = answer.id,
            userId = answer.user.id,
            wordId = answer.word.id,
            created_at = answer.createdAt.time,
            updated_at = answer.updatedAt.time,
            histories = answer.histories.map { history ->
                Answer.History.Model(
                    impact_review = history.impactReview.id,
                    result = history.result,
                    post_at = history.postAt.time
                )
            } as MutableList<Answer.History.Model>
        ))
    }

    /** データベースの回答を更新*/
    fun updateAnswer(vararg answers: Answer) {
       answers.forEach { answer ->
           session.updateOne(
               Answer.Model::_id eq answer.id,
               Answer.Model::userId setTo  answer.user.id,
               Answer.Model::wordId setTo  answer.word.id,
               Answer.Model::histories setTo answer.histories.map {history ->
                   Answer.History.Model(
                       impact_review = history.impactReview.id,
                       result = history.result,
                       post_at = history.postAt.time
                   )
               },
               Answer.Model::updated_at setTo Date
                   .from(
                       LocalDateTime
                           .now()
                           .atZone(DefaultZone)
                           .toInstant()).time)
       }
    }

}


fun Route.answers() {

}
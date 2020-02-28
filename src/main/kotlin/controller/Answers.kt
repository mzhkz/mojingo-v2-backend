package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Answer
import com.aopro.wordlink.database.model.Review
import com.aopro.wordlink.database.model.User
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.utilities.CurrentUnixTime
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
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
                createdAt = model.created_at,
                updatedAt = model.updated_at,
                histories = model.histories
                    .map { historyModel ->
                        Answer.History(
                            impactReviewId = historyModel.impact_review,
                            result = historyModel.result,
                            postAt = historyModel.post_at
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
        return if (answers.none { answer -> answer.id == builder }) builder else generateNoDuplicationId()
    }

    /** 回答をデータベースに記録*/
    fun insertAnswer(answer: Answer) {
        session.insertOne(Answer.Model(
            _id = answer.id,
            userId = answer.user.id,
            wordId = answer.word.id,
            created_at = answer.createdAt,
            updated_at = answer.updatedAt,
            histories = answer.histories.map { history ->
                Answer.History.Model(
                    impact_review = history.impactReviewId,
                    result = history.result,
                    post_at = history.postAt
                )
            } as MutableList<Answer.History.Model>
        ))
        answers.add(answer)
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
                       impact_review = history.impactReviewId,
                       result = history.result,
                       post_at = history.postAt
                   )
               },
               Answer.Model::updated_at setTo CurrentUnixTime
           )
       }
    }
}

fun User.getAnswer(word: Word): Answer {
    val already = Answers.answers().find { answer -> answer.user.id == this.id && answer.word.id == word.id }
    return if (already != null) {
        already
    } else {
        val newInstance = Answer(
            id = Answers.generateNoDuplicationId(),
            user = this,
            word = word,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime,
            histories = mutableListOf()
        )
        Answers.insertAnswer(newInstance)
        newInstance
    }
}


fun Route.answers() {

}
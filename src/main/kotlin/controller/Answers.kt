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


    private fun adapt(model: Answer.Model) =
        Answer(
            id = model._id,
            user = Users.users().find { user -> user.id == model.userId } ?: User.notExistObject(),
            word = Words.words().find { word -> word.id == model.word_id } ?: Word.notExistObject(),
            rank = model.rank,
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

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Answer.Model>("answers")

        answers.addAll(session.find().map { model->
            adapt(model)
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
            word_id = answer.word.id,
            rank = answer.rank,
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
               Answer.Model::rank setTo answer.rank,
               Answer.Model::word_id setTo  answer.word.id,
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

    /** 今日、確認、出題する問題 */
    fun pickupRecommended(user: User): List<Word> {
        return Answers.answers().mapNotNull { answer ->
            if (answer.histories.isNotEmpty() && isExamWordWithAnswer(answer)) {
                answer.word
            } else {
                null
            }
        }
    }


    /**
     * 追加前にランク変動かどうか検証する
     */
    fun isExamWordWithAnswer(target: Answer): Boolean {
        val lately = target.histories.map { history ->  history.postAt }.max() ?: 0L
        val diff = CurrentUnixTime - lately //差分を出す
        return diff >= getDelay(target.rank)
    }

    /** 最後に問題を解いた日から何日後に出題するか */
    private fun getDelay(rank: Int): Long {
        val hour: Long = 60 * 60
        val day: Long = 60 * 60 * 24 //1日
        val week: Long = day * 7 //7日
        val month: Long = day * 30 //30日

        return when (rank) {
            0 -> hour * 20 //1日
            1 -> hour * 20 //2日後
            2 -> day * 3 //4日後
            3 -> day * 6 //1週間後
            4 -> week //11日後
            5 -> week + day * 3 //2週間後
            6 -> week * 2 //3週間後
            7 -> week * 3 //1か月後
            8 -> month
            else -> month * 2 //3カ月後
        }
    }
}

fun User.getAnswer(word: Word): Answer {
    val already = Answers.answers().find { answer -> answer.user.id == this.id && answer.word.name == word.name }
    return if (already != null) {
        already
    } else {
        val newInstance = Answer(
            id = Answers.generateNoDuplicationId(),
            user = this,
            word = word,
            rank = 0,
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
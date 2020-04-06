package me.mojingo.v2.backend

import com.google.gson.Gson
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import me.mojingo.v2.backend.controller.Answers
import me.mojingo.v2.backend.controller.Categories
import me.mojingo.v2.backend.controller.Users
import me.mojingo.v2.backend.database.DatabaseHandler
import me.mojingo.v2.backend.database.model.Answer
import me.mojingo.v2.backend.database.model.Word
import me.mojingo.v2.backend.utilities.CurrentUnixTime
import me.mojingo.v2.backend.utilities.toBase64
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import java.io.File
import java.nio.charset.Charset

object InheritExecuter {

    fun json() {
        val json = File("./target1900+.json")
        val file = File("./old-json.excel")
        file.createNewFile()
        file.setWritable(true)

        val word = Gson().fromJson(json.readText(Charset.defaultCharset()), Array<WordDataResponse>::class.java)

        var body = ""

        word.forEach {
            body += "${it.name},${it.mean},${it.memo}\n"
        }

        file.writeText(body)
    }

    data class WordDataResponse(
        val id: String,
        val name: String,
        val memo: String,
        val mean: String,
        val rank: Int,
        val tags: MutableList<String>,
        val created: Long
    )

    fun execute() {
        DatabaseHandler.initialize()
        DatabaseHandler.clientSession

        val v1Client = KMongo.createClient(MongoClientURI("mongodb+srv://application:mojingo123@mojingo-pp43m.gcp.mongodb.net/"))
        val v1Database = v1Client.getDatabase("mojingo")
        val v1Answers = mutableListOf<v1Answer>()

        val  acollection = v1Database.getCollection<v1AnswerModel>("answers")
        acollection.find().forEach {
            val answer =  presentAnswer(it)
            v1Answers.add(answer)
        }


        val v1Words = mutableListOf<v1Word>()
        val  wcollection = v1Database.getCollection<v1WordModel>("words")
        wcollection.find().forEach {
            val words =  presentWord(it)
            v1Words.add(words)
        }



        val filterW = v1Words.filter { it.owner == "109593756084581831456" }

        val file = File("./old-json.excel")
        file.createNewFile()
        file.setWritable(true)

        println("${filterW.size}個のターゲットが見つかりました")

        var body = ""

        val categoryId = Categories.generateNoDuplicationId()

        println("${categoryId}で登録を回診します")

        val enroll = v1Answers.filter { answer -> filterW.any { word -> word.id == answer.word } }
        enroll.forEach {
            if (it.rank != 0) {

                val word = filterW.find { word -> word.id == it.word }!!

                body += "${word.name},${word.mean},${word.memo}\n"


                Answers.session.insertOne(Answer.Model(
                    _id = Answers.generateNoDuplicationId(),
                    userId = Users.users().find { user -> user.username == "riku-m" }!!.id,
                    word_id = (categoryId + "||" + word.name).toBase64() ,
                    rank = it.rank,
                    created_at = CurrentUnixTime,
                    updated_at = CurrentUnixTime,
                    histories = it.analytics.map { analytics ->
                        Answer.History.Model(
                            impact_review = "NONE",
                            result = analytics.result,
                            post_at = analytics.created
                        )
                    } as MutableList<Answer.History.Model>
                ))
            }
        }

        file.writeText(body)

        println("完了！！")



}



data class v1Answer(
    val id: String = "",
    val word: String,
    val respondent: String,
    var rank: Int, //覚えているランク
    var analytics: MutableList<AnswerAnalytics> = mutableListOf(),
    val created: Long,
    var updated: Long
)

    /** ランクを設定する*/
    fun v1Answer.setRank(newRank: Int) {
        if (newRank >= 0) {
            rank = newRank
        }
    }

    fun v1Answer.toModel() = v1AnswerModel(
        _id = id,
        word_id = word,
        respondent_id = respondent,
        rank = rank,
        analytics = analytics.map { analytics ->
            AnswerAnalyticsModel(
                result = analytics.result,
                created = analytics.created
            )
        }.toMutableList(),
        created = created,
        updated = updated
    )

    data class v1AnswerModel(
        val _id: String = "",
        val word_id: String = "",
        val respondent_id: String = "",
        var rank: Int = 0, //覚えているランク
        val analytics: MutableList<AnswerAnalyticsModel> = mutableListOf(),
        val created: Long = 0,
        val updated: Long = 0
    )

    fun presentAnswer(model: v1AnswerModel) = v1Answer(
        id = model._id,
        word = model.word_id,
        respondent = model.respondent_id,
        rank = model.rank,
        analytics = model.analytics.map { presentAnswerAnalytics(it) }.toMutableList(),
        created = model.created,
        updated = model.updated
    )

    data class AnswerAnalytics(
        val created: Long,
        val answer: String,
        val result: Int //1: 正解 0: ミス
    )

    data class AnswerAnalyticsModel(
        val created: Long = 0,
        val answer: String = "",
        val result: Int = 0
    )

    fun presentAnswerAnalytics(model: AnswerAnalyticsModel) = AnswerAnalytics(
        result = model.result,
        answer = model.answer,
        created = model.created
    )
}

data class v1Word(
    val id: String,
    var name: String,
    var mean: String,
    var memo: String = "",
    var tags: MutableList<String> = mutableListOf(),
    val owner: String)



data class v1WordModel(
    val _id: String = "",
    val english: String = "",
    val japanese: String = "",
    val memo: String = "",
    val tags: MutableList<String> = mutableListOf(),
    val owner: String = ""
)


fun presentWord(model: v1WordModel) = v1Word(
    id = model._id,
    name = model.english,
    mean = model.japanese,
    memo = model.memo,
    tags = model.tags,
    owner = model.owner
)
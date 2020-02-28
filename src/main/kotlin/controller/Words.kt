package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.requireNotNullAndNotEmpty
import com.aopro.wordlink.utilities.CurrentUnixTime
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.time.LocalDateTime
import java.util.*

object Words {

    private val words = mutableListOf<Word>()
    private lateinit var session: MongoCollection<Word.Model>

    fun words() = words.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Word.Model>("words")

        val cacheHash = hashMapOf<String, Int>()

        words.addAll(session.find().map { model ->
            var cacheMapNumber = cacheHash[model.category_id] ?: 0
            if (cacheMapNumber == null) {
                cacheHash[model.category_id] = 0
            }
            cacheHash[model.category_id] = cacheMapNumber + 1

            Word(
                id = model._id,
                number = cacheMapNumber + 1,
                name = model.name,
                mean = model.mean,
                category = Categories.categories().find { category -> category.id == model.category_id } ?: Category.notExistObject(),
                createdAt = model.created_at,
                updatedAt = model.updated_at
            )
        })
    }

    tailrec fun generateId(): String {
        val length = 8
        var temp = ""
        val elements = ensureIdElemments.toMutableList()
        for (i in 0..length) {
            temp += elements.random()
        }

        return if (words.filter { word -> word.id == temp }.isEmpty()) temp else generateId()
    }


    /**　データベースにデータを挿入する*/
    fun insertWord(word: Word) {
        session.insertOne(Word.Model(
            _id = word.id,
            name = word.name,
            mean = word.mean,
            category_id = word.category.id,
            created_at = word.createdAt,
            updated_at = word.updatedAt
        ))
        words.add(word)
    }

    /** データを更新する */
    fun updateWord(word: Word) {
        session.updateOne(
            Word.Model::_id eq word.id,
            Word.Model::name setTo word.name,
            Word.Model::mean setTo word.mean,
            Word.Model::category_id setTo word.category.id,
            Word.Model::created_at setTo word.createdAt,
            Word.Model::updated_at setTo CurrentUnixTime
        )
    }
}
@Location("/word")
class WordRoute {

    @Location("/d/:id")
    data class Get(val id: String)

    @Location("/update")
    class Update {
        data class Payload(
            val id: String = "",
            val name: String = "",
            val means: String = ""
        )
    }

}

fun Route.word() {


    get<WordRoute.Get> { query ->
        context.request.tokenAuthentication()
        val target = Words.words().find { word ->  query.id == word.id} ?: throw BadRequestException("Not found '${query.id}' as word.")

        context.respond(ResponseInfo(data = target))
    }

    post<WordRoute.Update> {
        val payload = context.receive(WordRoute.Update.Payload::class)
        requireNotNullAndNotEmpty(payload.id, payload.means, payload.name)

        val target = Words.words().find { word ->  payload.id == word.id} ?: throw BadRequestException("Not found '${payload.id}' as word.")

        target.apply {
            name = payload.name
            mean = payload.means
        }

        Words.updateWord(word = target)

        context.respond(ResponseInfo(message = "has been succeed."))

    }

}
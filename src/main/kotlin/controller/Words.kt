package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.database.model.readWordCSV
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.routing.Route
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
            var cacheMapNumber = cacheHash[model.category_id]
            if (cacheMapNumber == null) {
                cacheMapNumber = 1
                cacheHash[model.category_id] = cacheMapNumber
            }

            Word(
                id = model._id,
                number = cacheMapNumber,
                name = model.name,
                mean = model.mean,
                category = Categories.categories().find { category -> category.id == model.category_id } ?: Category.notExistObject(),
                createdAt = Date(model.created_at * 1000),
                updatedAt = Date(model.updated_at * 1000)
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
    fun insertWord(words: MutableList<Word>) {
        words.forEach { word ->
            session.insertOne(Word.Model(
                _id = word.id,
                name = word.name,
                mean = word.mean,
                category_id = word.category.id,
                created_at = word.createdAt.time,
                updated_at = word.updatedAt.time
            ))
        }
    }

    /** データを更新する */
    fun updateWord(word: Word) {
        session.updateOne(
            Word.Model::_id eq word.id,
            Word.Model::name setTo word.name,
            Word.Model::mean setTo word.mean,
            Word.Model::category_id setTo word.category.id,
            Word.Model::created_at setTo word.createdAt.time,
            Word.Model::updated_at setTo Date.from(
                LocalDateTime.now().atZone(DefaultZone).toInstant()
            ).time
        )
    }
}
@Location("/word")
class WordRoute {

    @Location("/import")
    class Import {
        data class Payload(val categoryId: String = "", val csvFileBody: MutableList<String> = mutableListOf())
    }

}

fun Route.word() {

    post<WordRoute.Import> {
        context.request.tokenAuthentication(2) //管理者レベルからアクセス可能
        val payload = context.receive<WordRoute.Import.Payload>()
        val category = Categories.categories().find { category -> category.id == payload.categoryId } ?: throw BadRequestException("INVALID CATEGORY_ID")

        val word = readWordCSV(payload.csvFileBody, category)

    }

}
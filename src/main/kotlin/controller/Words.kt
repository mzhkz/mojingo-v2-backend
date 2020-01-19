package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.database.model.readWordCSV
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.routing.Route
import org.litote.kmongo.getCollection
import java.util.*

object Words {

    private val words = mutableListOf<Word>()
    private lateinit var session: MongoCollection<Word.Model>

    fun words() = words.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Word.Model>("words")

        words.addAll(session.find().map { model ->
            Word(
                id = model._id,
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
        val payload = context.receive<WordRoute.Import.Payload>()
        val category = Categories.categories().find { category -> category.id == payload.categoryId } ?: throw BadRequestException("INVALID CATEGORY_ID")

        val word = readWordCSV(payload.csvFileBody, category)

    }

}
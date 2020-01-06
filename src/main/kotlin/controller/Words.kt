package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Word
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
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
                categoryId = model.category_id,
                createdAt = Date(model.created_at * 1000),
                updatedAt = Date(model.updated_at * 1000),
                updatedBy = model.updated_by
            )
        })
    }
}

@Location("/word")
class WordRoute {

}

fun Route.word() {

}
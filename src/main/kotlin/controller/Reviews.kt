package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Review
import com.aopro.wordlink.database.model.Word
import com.mongodb.client.MongoCollection
import org.litote.kmongo.getCollection
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

}
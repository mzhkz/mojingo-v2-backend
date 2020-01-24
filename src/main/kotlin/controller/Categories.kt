package com.aopro.wordlink.controller

import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.readWordCSV
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
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

object Categories {

    private val categories = mutableListOf<Category>()
    private lateinit var session: MongoCollection<Category.Model>

    fun categories() = categories.toMutableList()

        fun initialize() {
            session = DatabaseHandler
                .databaseSession
                .getCollection<Category.Model>("categories")

            categories.addAll(session.find().map{ model ->
                Category(
                    id = model._id,
                    name = model.name,
                    description = model.description,
                    createdAt = Date(model.created_at * 1000),
                    updatedAt = Date(model.updated_at * 1000),
                    private = model.private
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

        return if (categories.filter { category -> category.id == builder }.isEmpty()) builder else generateNoDuplicationId()
    }

    /** データベースにデータを追加*/
    fun insertCategory(category: Category) {
        session.insertOne(Category.Model(
            _id = category.id,
            name = category.name,
            description = category.description,
            created_at = category.createdAt.time,
            updated_at = category.createdAt.time,
            private = category.private
        ))
    }


    /** データベースを更新*/
    fun updateCategory(vararg categories: Category) {
        categories.forEach { category ->
            session.updateOne(
                Category.Model::_id eq category.id,
                Category.Model::name setTo  category.name,
                Category.Model::description setTo  category.description,
                Category.Model::private setTo  category.private,
                Category.Model::updated_at setTo Date
                    .from(LocalDateTime
                        .now()
                        .atZone(DefaultZone)
                        .toInstant()).time)
        }
    }
}

@Location("categories")
class CategoryRoute {

    class Create() {
        data class Payload(
            val name: String,
            val description: String,
            val private: Boolean,
            val csvBody: String
        )
    }

}

fun Route.category() {

    get {
        val user = context.request.tokenAuthentication()
        val categories = Categories.categories()
        val response = categories.mapNotNull { c1 ->
            categories.find { c2 -> c1.id == c2.id }
        }
        context.respond(ResponseInfo(data = response))
    }

    post<CategoryRoute.Create> {
        context.request.tokenAuthentication(2) //管理者レベルからアクセス可能

        val payload = context.receive(CategoryRoute.Create.Payload::class)

        val instance = Category(
            id = Categories.generateNoDuplicationId(),
            name = payload.name,
            description = payload.description,
            private = payload.private,
            createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
            updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
        )


        val entries = readWordCSV(
            line = payload.csvBody.split(";").toMutableList(),
            category = instance
        )


    }
}
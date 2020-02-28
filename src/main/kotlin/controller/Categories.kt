package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.requireNotNullAndNotEmpty
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.ensureIdElemments
import com.aopro.wordlink.utilities.maximumAsPagination
import com.aopro.wordlink.utilities.splitAsPagination
import com.google.gson.annotations.Expose
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
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
        categories.add(category)
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

    data class CategoryResponse(
        @Expose val category: Category? = null,
        @Expose val wordCount: Int = 0,
        @Expose val maxPageSize: Int = 0
    )

    @Location("/create")
    class Create {
        data class Payload(
            @Expose val name: String = "",
            @Expose val description: String = "",
            @Expose val private: Boolean = false,
            @Expose val csvBody: MutableList<String> = mutableListOf()
        )
    }

    @Location("/view/{id}")
    data class View(val id: String = "") {

        @Location("/words")
        class Words
    }

}

fun Route.category() {

    get<CategoryRoute> {
        val user = context.request.tokenAuthentication()
        val categories = Categories.categories()
        val response = categories.mapNotNull { c1 ->
            categories.find { c2 -> c1.id == c2.id }
        }.map { c3 ->
            CategoryRoute.CategoryResponse(
                category = c3,
                wordCount = Words.words().filter { word -> word.category.id == c3.id }.size
            )
        }
        context.respond(ResponseInfo(data = response))
    }

    post<CategoryRoute.Create> {
        context.request.tokenAuthentication(2) //管理者レベルからアクセス可能

        val payload = context.receive(CategoryRoute.Create.Payload::class)
        requireNotNullAndNotEmpty(payload.name, payload.csvBody) //Null and Empty Check!

        val instance = Category(
            id = Categories.generateNoDuplicationId(),
            name = payload.name,
            description = payload.description,
            private = payload.private,
            createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
            updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
        )

        val entries = convertExcelFileToWords(
            line = payload.csvBody,
            category = instance
        )

        Categories.insertCategory(instance)
        entries.forEach { word -> Words.insertWord(word) }

        context.respond(ResponseInfo(message = "has been succeed"))

        println(entries.size)
        println(Words.words().size)
    }

    get<CategoryRoute.View> {
        context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        requireNotNullAndNotEmpty(categoryId) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")

        val words = Words.words().filter { word -> word.category.id == target.id }

        context.respond(ResponseInfo(data = CategoryRoute.CategoryResponse(
            category = target,
            maxPageSize = words.maximumAsPagination(25)
        )))

    }

    get<CategoryRoute.View.Words> {
        context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        val page = context.request.queryParameters["page"]?.toInt() ?: 1
        val keyword = context.request.queryParameters["keyword"] ?: ""

        requireNotNullAndNotEmpty(categoryId, page, keyword) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?:throw BadRequestException("Not correct category_id")

        val words = Words.words().filter { word -> word.category.id == target.id && keyword.indexOf(keyword) != -1}

        context.respond(ResponseInfo(data = words.splitAsPagination(page = page, index = 25).toMutableList()))

    }

}

/** CSVファイルから単語を読み込む*/
fun convertExcelFileToWords(line: MutableList<String>, category: Category): MutableList<Word> {
    val regex = Regex("^(No)(,)(Name)(,)(Mean)\$") //No,Name,Mean
    var assignNumber = 0

    return line.mapNotNull { str ->
        val match = regex.matchEntire(str) //タイトルかどうか
        if (match == null && str.indexOf(",") > 0) {
            val contants = str.split(",")
            assignNumber+=1
            Word(
                id = Words.generateId(),
                number = assignNumber,
                name = contants[1],
                mean = contants[2],
                category = category,
                createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
                updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
            )
        } else null
    }.toMutableList()

}
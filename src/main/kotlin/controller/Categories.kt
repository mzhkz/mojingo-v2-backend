package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.requireNotNullAndNotEmpty
import com.aopro.wordlink.utilities.*
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
import java.lang.Exception
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
                    spreadSheetId = model.spread_sheet_id,
                    description = model.description,
                    createdAt = model.created_at,
                    updatedAt = model.updated_at,
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
            spread_sheet_id = category.spreadSheetId,
            description = category.description,
            created_at = category.createdAt,
            updated_at = category.createdAt,
            private = category.private
        ))
        categories.add(category)
    }


    /** データベースを更新*/
    fun updateCategory(category: Category) {
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

    /**
     * データベースから単語を削除する
     */
    fun deleteCategory(category: Category) {
        session.deleteOne(Category.Model::_id eq category.id)
        categories.removeIf { categ -> categ.id == category.id }
    }
}

@Location("categories")
class CategoryRoute {

    data class CategoryResponse(
        @Expose val category: Category? = null,
        @Expose val wordCount: Int = 0,
        @Expose val createdAgo: String = ""
    )

    @Location("/create")
    class Create {
        data class Payload(
            @Expose val name: String = "",
            @Expose val description: String = "",
            @Expose val private: Boolean = false,
            @Expose val sheetId: String = ""
        )
    }

    @Location("/view/{id}")
    data class View(val id: String = "") {

        @Location("/words")
        class Words {
            data class CategoryWordsResponse(
                @Expose val body: MutableList<Word> = mutableListOf(),
                @Expose val pageSize: Int = 0
            )
        }

        @Location("update")
        class Update {
            data class Payload(
                @Expose val name: String = "",
                @Expose val description: String = ""
            )
        }

        @Location("sync")
        class Sync

        @Location("delete")
        class Delete {
            data class Payload(
                @Expose val withDepend: Boolean = true
            )
        }
    }

}

fun Route.category() {

    get<CategoryRoute> {
        val user = context.request.tokenAuthentication()
        val categories = Categories.categories()
        val response = categories.mapNotNull { c1 ->
            categories.find { c2 -> c1.id == c2.id }
        }.reversed().map { c3 ->
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
        requireNotNullAndNotEmpty(payload.name, payload.sheetId) //Null and Empty Check!

        val instance = Category(
            id = Categories.generateNoDuplicationId(),
            name = payload.name,
            spreadSheetId = payload.sheetId,
            description = payload.description,
            private = payload.private,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )
       try {
           Words.asyncBySheet(target = instance)
       } catch (e: Exception) {
           e.printStackTrace()
           throw BadRequestException("sheet-request@mojingo-v2-prod.iam.gserviceaccount.com")
       }
        Categories.insertCategory(instance)

        context.respond(ResponseInfo(message = "has been succeed"))
    }

    post<CategoryRoute.View.Sync> {
        context.request.tokenAuthentication(2) //管理者レベルからアクセス可能
        val categoryId = context.parameters["id"]
        requireNotNullAndNotEmpty(categoryId) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")
        try {
            Words.asyncBySheet(target = target)
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadRequestException("sheet-request@mojingo-v2-prod.iam.gserviceaccount.com")
        }

        context.respond(ResponseInfo(message = "has been succeed"))
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
            createdAgo = target.createdAt.currentUnixTimediff()
        )))

    }

    post<CategoryRoute.View.Update> {
        context.request.tokenAuthentication(2)
        val categoryId = context.parameters["id"]
        val payload = context.receive(CategoryRoute.View.Update.Payload::class)
        requireNotNullAndNotEmpty(categoryId, payload.name, payload.description) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")

       Categories.updateCategory(target.apply {
           name = payload.name
           description = payload.description
       })

        context.respond(ResponseInfo(message = "has been succeed"))
    }


    post<CategoryRoute.View.Delete> {
        context.request.tokenAuthentication(3)
        val categoryId = context.parameters["id"]
        val payload = context.receive(CategoryRoute.View.Delete.Payload::class)
        requireNotNullAndNotEmpty(categoryId, payload.withDepend) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")

        context.respond(ResponseInfo(message = "has been succeed"))

    }

    get<CategoryRoute.View.Words> {
        context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        val page = context.request.queryParameters["page"]?.toInt() ?: 1
        val keyword = context.request.queryParameters["keyword"] ?: ""

        requireNotNullAndNotEmpty(categoryId, page, keyword) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?:throw BadRequestException("Not correct category_id")

        val words = Words.words().filter { word -> word.category.id == target.id && word.name.indexOf(keyword) != -1 }

        context.respond(ResponseInfo(data = CategoryRoute.View.Words.CategoryWordsResponse(
            body = words.splitAsPagination(page = page, index = 25).toMutableList(),
            pageSize = words.maximumAsPagination(25)
        )))
    }

}
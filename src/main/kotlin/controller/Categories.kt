package me.mojingo.v2.backend.controller

import me.mojingo.v2.backend.database.DatabaseHandler
import me.mojingo.v2.backend.database.model.Category
import me.mojingo.v2.backend.database.model.Word
import me.mojingo.v2.backend.utilities.*
import com.google.gson.annotations.Expose
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import me.mojingo.v2.backend.*
import me.mojingo.v2.backend.database.model.User
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.lang.Exception
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

object Categories {

    private val categories = mutableListOf<Category>()
    private lateinit var session: MongoCollection<Category.Model>

    fun categories() = categories.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<Category.Model>("categories")

        categories.addAll(session.find().map { model ->
            Category(
                id = model._id,
                name = model.name,
                spreadSheetId = model.spread_sheet_id,
                description = model.description,
                owner = Users.users().find { usr -> usr.id == model.owner_id } ?: User.notExistObject(),
                shareUsers = model.share_users.mapNotNull { usrId ->
                    Users.users().find { usr -> usr.id == usrId }
                }.toMutableList(),
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
        session.insertOne(
            Category.Model(
                _id = category.id,
                name = category.name,
                spread_sheet_id = category.spreadSheetId,
                description = category.description,
                owner_id = category.owner.id,
                share_users = category.shareUsers.map { usr -> usr.id }.toMutableList(),
                created_at = category.createdAt,
                updated_at = category.createdAt,
                private = category.private
            )
        )
        categories.add(category)
    }


    /** データベースを更新*/
    fun updateCategory(category: Category) {
        session.updateOne(
            Category.Model::_id eq category.id,
            Category.Model::name setTo category.name,
            Category.Model::description setTo category.description,
            Category.Model::share_users setTo category.shareUsers.map { usr -> usr.id },
            Category.Model::private setTo category.private,
            Category.Model::updated_at setTo Date
                .from(
                    LocalDateTime
                        .now()
                        .atZone(DefaultZone)
                        .toInstant()
                ).time
        )
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
            @Expose val private: Boolean = true,
            @Expose val sheetId: String = ""
        )
    }

    @Location("/view/{id}")
    data class View(val id: String = "") {

        @Location("/words")
        class Words {
            data class CategoryWordsResponse(
                @Expose val body: MutableList<HashMap<String, *>> = mutableListOf(),
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
        val authUser = context.request.tokenAuthentication()
        val categories = Categories.categories()
        val response =
            categories.filter { category -> category.shareUsers.contains(authUser) }.reversed().map { category ->
                CategoryRoute.CategoryResponse(
                    category = category,
                    wordCount = Words.words().filter { word -> word.category.id == category.id }.size
                )
            }
        context.respond(ResponseInfo(data = response))
    }

    post<CategoryRoute.Create> {
        val authUser = context.request.tokenAuthentication()
        if (authUser.id == ApplicationConfig.SYSTEM_ROOT_NAME) //rootアカウント
            throw BadRequestException("Rootアカウントで辞書の作成は出来ません.")

        val payload = context.receive(CategoryRoute.Create.Payload::class)
        requireNotNullAndNotEmpty(payload.name, payload.sheetId) //Null and Empty Check!

        val instance = Category(
            id = Categories.generateNoDuplicationId(),
            name = payload.name,
            spreadSheetId = payload.sheetId,
            description = payload.description,
            owner = authUser,
            shareUsers = mutableListOf(authUser), //ownerを追加
            private = payload.private,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )
        try {
            Words.asyncBySheet(target = instance)
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadRequestException("Error occur: ${e.localizedMessage}")
        }
        Categories.insertCategory(instance)

        context.respond(ResponseInfo(message = "has been succeed"))
    }

    post<CategoryRoute.View.Sync> {
        val authUser = context.request.tokenAuthentication() //管理者レベルからアクセス可能
        val categoryId = context.parameters["id"]
        requireNotNullAndNotEmpty(categoryId) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")
        if (!target.shareUsers.contains(authUser))
            throw AuthorizationException("同期する権限がありません。")
        try {
            Words.asyncBySheet(target = target)
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadRequestException("Error occur: ${e.localizedMessage}")
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

        context.respond(
            ResponseInfo(
                data = CategoryRoute.CategoryResponse(
                    category = target,
                    createdAgo = target.createdAt.currentUnixTimediff()
                )
            )
        )

    }

    post<CategoryRoute.View.Update> {
        val authUser = context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        val payload = context.receive(CategoryRoute.View.Update.Payload::class)
        requireNotNullAndNotEmpty(categoryId, payload.name, payload.description) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")
        if (!target.shareUsers.contains(authUser))
            throw AuthorizationException("更新する権限がありません。")

        if (target.private && target.owner.id != authUser.id)

            Categories.updateCategory(target.apply {
                name = payload.name
                description = payload.description
            })

        context.respond(ResponseInfo(message = "has been succeed"))
    }


    post<CategoryRoute.View.Delete> {
        val authUser = context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        val payload = context.receive(CategoryRoute.View.Delete.Payload::class)
        requireNotNullAndNotEmpty(categoryId, payload.withDepend) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")
        if (target.owner.id != authUser.id)
            throw AuthorizationException("削除する権限がありません。")

        Categories.deleteCategory(category = target)

        context.respond(ResponseInfo(message = "has been succeed"))

    }

    get<CategoryRoute.View.Words> {
        val authUser = context.request.tokenAuthentication()
        val categoryId = context.parameters["id"]
        val page = context.request.queryParameters["page"]?.toInt() ?: 1
        val keyword = context.request.queryParameters["keyword"] ?: ""

        requireNotNullAndNotEmpty(categoryId, page, keyword) //Null and Empty Check!

        val target = Categories.categories().find { category -> category.id == categoryId }
            ?: throw BadRequestException("Not correct category_id")
        if (!target.shareUsers.contains(authUser))
            throw AuthorizationException("表示する権限がありません。")

        var words = listOf<Word>()

        Words.tagValidates.forEachIndexed { index, validate ->
            val matched = validate.matchEntire(keyword)

            when (index) {
                0 -> {
                    if (matched != null) {
                        val min = matched.groupValues[2].toInt()
                        val max = matched.groupValues[4].toInt()

                        words = Words.words().filter { word -> word.category.id == target.id && word.number in min..max }
                        return@forEachIndexed
                    }
                }
                1 -> {
                    words = Words.words().filter { word -> word.category.id == target.id }
                        .map { word -> word to (word.name.indexOf(keyword) + word.mean.indexOf(keyword))  }
                        .filter { pair -> pair.second >= -1 }
                        .map { pair -> pair.first }
                    return@forEachIndexed
                }
            }
        }

        context.respond(ResponseInfo(data = CategoryRoute.View.Words.CategoryWordsResponse(
            body = words.sortedBy { word -> word.number }.splitAsPagination(page = page, index = 25).map { word ->
                hashMapOf(
                    "id" to word.id,
                    "name" to word.name,
                    "number" to word.number,
                    "mean" to word.mean,
                    "description" to word.description,
                    "rank" to authUser.getAnswer(word).rank
                )
            }.toMutableList(),
            pageSize = words.maximumAsPagination(25)
        )))
    }

}
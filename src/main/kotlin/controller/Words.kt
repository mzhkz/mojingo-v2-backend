package me.mojingo.v2.backend.controller

import me.mojingo.v2.backend.BadRequestException
import me.mojingo.v2.backend.ResponseInfo
import me.mojingo.v2.backend.database.DatabaseHandler
import me.mojingo.v2.backend.database.model.*
import me.mojingo.v2.backend.requireNotNullAndNotEmpty
import me.mojingo.v2.backend.utilities.*
import com.google.cloud.texttospeech.v1.*
import com.google.gson.annotations.Expose
import com.google.rpc.BadRequest
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.caseInsensitiveMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.mojingo.v2.backend.ApplicationConfig
import org.litote.kmongo.getCollection
import java.io.File

object Words {

    private val words = mutableListOf<Word>()
    val tagValidates = mutableListOf(
        Regex("^(num )([\\d]+)( )([\\d]+)\$"),
        Regex("^*\$")
    )

    fun words() = words.toMutableList()

    fun initialize() {
        Categories.categories().forEach { category ->
            asyncBySheet(category)
        }
    }

    /** Spreadsheetと同期 */
    fun asyncBySheet(target: Category) {
        val presenceCheck = mutableListOf<String>()
        val readResult = GoogleAPI.setUpSheet.Spreadsheets().values().get(target.spreadSheetId, "A1:C").execute()
        val entries = readResult.getValues().mapIndexed { index, line ->
            Word(
                id = "${target.id}||${line.getOrNull(0)}".toBase64(),
                number = index + 1,
                name = "${line.getOrNull(0)}",
                mean = "${line.getOrNull(1)}",
                description = if (line.size > 2) "${line.getOrNull(2)}" else "",
                category = target
            )
        }

        entries.forEach { entry ->
            val already = words.find { word -> word.name == entry.name && word.category.id == target.id }
            if (already == null) {
                words.add(entry)
            } else {
                already.apply {
                    number = entry.number
                    mean = entry.mean
                    description = entry.description
                }
            }
            presenceCheck.add(entry.name)
        }

        words.filter { word -> word.category.id == target.id && !presenceCheck.contains(word.name) }
            .forEach { delete ->
                words.removeIf { word -> word.category.id == target.id && word.name == delete.name }
                delete.apply {
                    number = -1 //number of category
                    name = "-- 存在しない単語 --"
                    mean = ""
                    category = Category.notExistObject()
                    description = ""
                }
            }
    }

    /** Google Text-To-Speech-APIを使用して発音のMP3ファイルを生成する*/
    fun getSpeechMP3(word: Word, language: Language): File {
        val fileName = "${(word.category.id + "||" + word.name).toBase64()}-speech.mp3"
        val cache = File("./temperature/$fileName")
        if (cache.exists()) //音声キャッシュが存在した場合はキャッシュを返す
            return cache
        else {
            val textToSpeechClient = TextToSpeechClient.create()
            val audioContent = textToSpeechClient.synthesizeSpeech(
                SynthesisInput.newBuilder()
                    .setText(word.name)
                    .build(),
                VoiceSelectionParams.newBuilder()
                    .setLanguageCode(language.code)
                    .setSsmlGender(SsmlVoiceGender.MALE)
                    .build(),
                AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build()
            ).audioContent
            cache.createNewFile() //ファイル生成
            cache.outputStream().write(audioContent.toByteArray())
            return cache
        }
    }

}

enum class Language(val code: String) {
    Japanese("ja-JP"),
    English("en-US");

    companion object {
        fun languageCode(code: String) = Language.values().find { it.code == code } ?: English
    }
}

@Location("/words")
class WordRoute {

    @Location("/recommended")
    class Recommended {
        data class RecommendedResponse(
            @Expose val category: Category,
            @Expose val entriesSize: Int,
            @Expose val reviewSize: Int
        )

        data class Payload(@Expose val categoryId: String = "")
    }

    @Location("/search")
    class Search {
        data class SearchWordsResponse(
            @Expose val body: MutableList<HashMap<String, Any>> = mutableListOf(),
            @Expose val resultSize: Int = 0,
            @Expose val pageSize: Int = 0
        )
    }

    @Location("{category}/{name}")
    data class View(val category: String = "", val name: String = "") {

        @Location("/update")
        class Update {
            data class Payload(
                @Expose val id: String = "",
                @Expose val name: String = "",
                @Expose val means: String = ""
            )
        }
    }

    @Location("{wordId}/pronounce")
    data class Pronounce(val wordId: String = "")
}

fun Route.word() {


//    get<WordRoute.View> { query ->
//        context.request.tokenAuthentication()
//        val target = Words.words().find { word -> query.name == word.name && query.category == word.category.id }
//            ?: throw BadRequestException("Not found '${query.category}/${query.name}' as word.")
//
//        context.respond(ResponseInfo(data = target))
//    }

    get<WordRoute.Recommended> {
        val target = context.request.tokenAuthentication()
        target.refreshRecommended() //Refresh

        context.respond(
            ResponseInfo(
                data = target.cacheRecommended.mapNotNull { recommended ->
                    if (recommended.entries.size != 0) {
                        WordRoute.Recommended.RecommendedResponse(
                            category = recommended.category,
                            entriesSize = recommended.entries.size,
                            reviewSize = recommended.entries.maximumAsPagination(ApplicationConfig.REVIEW_OF_RECOMMENDED_MAX_SIZE)
                        )
                    } else null
                })
        )
    }

    post<WordRoute.Recommended> {
        val targetUser = context.request.tokenAuthentication()
        val payload = context.receive(WordRoute.Recommended.Payload::class)
        val target = targetUser.cacheRecommended.find { recommended -> recommended.category.id == payload.categoryId }
            ?: throw BadRequestException("Not fount ${payload.categoryId} as Category")
        val candidate = target.entries

        val entries = candidate.shuffled().splitAsPagination(1, 100)

        val review = Review(
            id = Reviews.generateNoDuplicationId(),
            name = "[R] ${target.category.name.toShortDecoration(6)} ${entries.size}問",
            description = "",
            owner = targetUser,
            entries = entries.toMutableList(),
            answers = mutableListOf(),
            finished = false,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )

        Reviews.insertReview(review) //DBに追加
        context.respond(
            ResponseInfo(
                data = review
            )
        )
    }


    get<WordRoute.Search> {
        val authUser = context.request.tokenAuthentication()
        val page = context.request.queryParameters["page"]?.toInt() ?: 1
        val keyword = context.request.queryParameters["keyword"] ?: ""

        requireNotNullAndNotEmpty(page) //Null and Empty Check!

        val words = if (keyword.isNotEmpty()) Words.words().filter { word -> word.category.shareUsers.contains(authUser) && word.name.indexOf(keyword) != -1 }
            .sortedBy { word -> word.number }
        else mutableListOf()

        context.respond(
            ResponseInfo(
                data = WordRoute.Search.SearchWordsResponse(
                    body = words.splitAsPagination(page = page, index = 25).toMutableList().map { word ->
                        hashMapOf(
                            "id" to word.id,
                            "name" to word.name,
                            "number" to word.number,
                            "mean" to word.mean,
                            "description" to word.description,
                            "category" to word.category,
                            "rank" to 0
                        )
                    }.toMutableList(),
                    resultSize = words.size,
                    pageSize = words.maximumAsPagination(25)
                )
            )
        )
    }

    get<WordRoute.Pronounce> { query ->
        context.request.tokenAuthentication()
        val pair = query.wordId.fromBase64().split("||")

        val language = context.request.queryParameters["language"] ?: "en-US"

        val target = Words.words().find { word -> pair[1] == word.name && pair[0] == word.category.id }
            ?: throw BadRequestException("Not found '${pair[0]}:${pair[1]}' as word.")

        context.respondFile(Words.getSpeechMP3(target, Language.languageCode(language)))
    }

}
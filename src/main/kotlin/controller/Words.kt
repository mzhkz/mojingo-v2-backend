package com.aopro.wordlink.controller

import com.aopro.wordlink.BadRequestException
import com.aopro.wordlink.ResponseInfo
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.Category
import com.aopro.wordlink.database.model.Word
import com.aopro.wordlink.requireNotNullAndNotEmpty
import com.aopro.wordlink.utilities.*
import com.google.cloud.texttospeech.v1.*
import com.google.gson.annotations.Expose
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.getCollection
import java.io.File

object Words {

    private val words = mutableListOf<Word>()

    fun words() = words.toMutableList()

    fun initialize() {
         GoogleAPI.setUpSheet
    }

    fun asyncBySheet(category: Category) {
        val readResult = GoogleAPI.setUpSheet.Spreadsheets().values().get(category.spreadSheetId, "A1:D").execute()
        val entries = readResult.getValues().mapIndexed { index, line ->
            Word(
                number  = index,
                name = line[0] as String,
                mean = line[1] as String,
                description = if (line.size > 2) line[2] as String else "",
                category = category
            )
        }
        words.addAll(entries)
    }

    /** Google Text-To-Speech-APIを使用して発音のMP3ファイルを生成する*/
    fun getSpeechMP3(word: Word, language: Language): File {
        val fileName = "${word.name}-${language.code}-speech.mp3"
        val cache = File("./temperature/$fileName")
        if (cache.exists()) //音声キャッシュが存在した場合はキャッシュを返す
            return cache
        else {
            val textToSpeechClient = TextToSpeechClient.create()
            runBlocking(Dispatchers.IO) {
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
            }
            return cache
        }
    }

}

enum class Language(val code: String) {
    Japanese("ja-JP"),
    English("en-US"),
}

@Location("/words")
class WordRoute {

    @Location("/search")
    class Search {
        data class SearchWordsResponse(
            @Expose val body: MutableList<Word> = mutableListOf(),
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

        @Location("pronounce")
        class Pronounce
    }
}

fun Route.word() {


    get<WordRoute.View> { query ->
        context.request.tokenAuthentication()
        val target = Words.words().find { word -> query.name == word.name && query.category == word.category.id }
            ?: throw BadRequestException("Not found '${query.category}/${query.name}' as word.")

        context.respond(ResponseInfo(data = target))
    }


    get<WordRoute.Search> {
        context.request.tokenAuthentication()
        val page = context.request.queryParameters["page"]?.toInt() ?: 1
        val keyword = context.request.queryParameters["keyword"] ?: ""

        requireNotNullAndNotEmpty(page) //Null and Empty Check!

        val words = if (keyword.isNotEmpty()) Words.words().filter { word -> word.name.indexOf(keyword) != -1 }
        else mutableListOf()

        context.respond(
            ResponseInfo(
                data = WordRoute.Search.SearchWordsResponse(
                    body = words.splitAsPagination(page = page, index = 25).toMutableList(),
                    resultSize = words.size,
                    pageSize = words.maximumAsPagination(25)
                )
            )
        )
    }

    get<WordRoute.View.Pronounce> {
        context.request.tokenAuthentication()
        val name = context.parameters["name"]
        val category = context.parameters["category"]
        val language = context.request.queryParameters["language"] ?: "en-US"

        val target = Words.words().find { word -> name == word.name && category == word.category.id }
            ?: throw BadRequestException("Not found '$category/$name' as word.")

        context.respond(Words.getSpeechMP3(target, Language.valueOf(language)))
    }

}
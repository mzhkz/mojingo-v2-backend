package me.mojingo.v2.backend

import me.mojingo.v2.backend.controller.*
import me.mojingo.v2.backend.database.DatabaseHandler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.Expose
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.request.header
import io.ktor.request.receiveText
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.*
import me.mojingo.v2.backend.utilities.toDecorateForDescription
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.event.Level
import java.io.File
import java.lang.IllegalArgumentException
import java.text.DateFormat

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {

    File("./temperature/").mkdirs()

    GoogleAPI.initialize()

    DatabaseHandler.initialize() //データベース初期化
    Users.initialize() //ユーザー読み込み
    Categories.initialize() //カテゴリーを読み込み
    Words.initialize()  // 単語データ読み込み
    Answers.initialize() //回答データ読み込み
    Reviews.initialize() //復習テストを読み込み

    install(Locations)

    install(Compression)

    install(ContentNegotiation) {
        gson {
            serializeNulls()
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
            excludeFieldsWithoutExposeAnnotation()
        }
    }


    install(DefaultHeaders) {
        header("X-Content-Type-Options", "SAMEORIGIN")
        header("X-Frame-Options", "SAMEORIGIN")
        header("X-XSS-Protection", "1; mode=block")
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Head)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)

        header("X-Access-Token")
        header("X-Requested-With")

        //ホスト設定
        val whitelist = mutableListOf<String>()
        whitelist.add(ApplicationConfig.FRONTEND_APP_DOMAIN)
        whitelist.forEach {host ->
            host(host, schemes = listOf("http", "https"))
        }

        allowCredentials = true
    }


    install(ForwardedHeaderSupport)


    routing {

        get {
            context.respond(ApplicationConfig.FRONTEND_APP_DOMAIN)
        }

        authentication()
        user()
        word()
        category()
        answers()
        reviews()

        install(StatusPages) {
            exception<AuthorizationException> { cause ->
                context.respond(ResponseInfo(result = HttpStatusCode.Unauthorized.value, message = cause.localizedMessage))
            }

            exception<NotFoundException> { cause ->
                context.respond(ResponseInfo(result = HttpStatusCode.NotFound.value, message = cause.localizedMessage))
            }

            exception<BadRequestException> { cause ->
                context.respond(ResponseInfo(result = HttpStatusCode.BadRequest.value, message = cause.localizedMessage))
            }

            exception<IllegalArgumentException> { cause ->
                context.respond(ResponseInfo(result = HttpStatusCode.BadRequest.value, message = "引数が要件を満たしていません。"))
            }

            exception<JsonSyntaxException> { cause ->
                context.respond(ResponseInfo(result = HttpStatusCode.BadRequest.value, message = "JSONオブジェクトを生成できません。"))
            }

            exception<Exception> { cause ->
                val stack = cause.stackTrace
                val message = "${cause.localizedMessage}#${stack[stack.size - 1]}"
                val hash = DigestUtils.sha512Hex(message).toUpperCase()

                if (context.request.header("X-Requested-With").equals("XMLHttpRequest")) {
                    context.response.header("X-Server-Error-Hash", hash) //ハッシュを含む
                    context.respond(ResponseInfo(result = 500, message = "サーバー側でエラーが発生しました", data = hash))
                } else {
                    context.respondHtml(block = {
                        head {
                            title { +"500 Internal Server Error" }
                        }
                        body {
                            h2 { +"サーバー側で処理中にエラーが発生しました" }
                            p { +"恐れ入りますが復旧まで今しばらくお待ちください。長時間経っても改善しない場合はお手数ですがシステム管理者に報告をお願いします。" }
                            p { +"報告の際は以下のテキストを含めてください。" }
                            code { +hash }
                        }
                    }, status = HttpStatusCode.InternalServerError)
                }
                cause.printStackTrace()

            }
        }
    }
}


data class ResponseInfo(
    @Expose val data: Any? = null,
    @Expose val result: Int = HttpStatusCode.OK.value,
    @Expose val message: String = "")


class AuthorizationException(error: String = "") : RuntimeException(error)
class NotFoundException(error: String = "") : RuntimeException(error)
class BadRequestException(error: String = "") : RuntimeException(error)

fun requireNotNullAndNotEmpty(vararg value: Any?) {
    value.forEach {
        if (it == null || it is String && value.isEmpty()) throw BadRequestException("入力不足の箇所があります。")
    }
}



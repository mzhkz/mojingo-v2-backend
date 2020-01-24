package com.aopro.wordlink

import com.aopro.wordlink.controller.*
import com.aopro.wordlink.database.DatabaseHandler
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.request.header
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import kotlinx.html.*
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.event.Level
import java.text.DateFormat

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {

    DatabaseHandler.initialize() //データベース初期化
    Categories.initialize() //カテゴリーを読み込み
    Users.initialize() //ユーザー読み込み
    Words.initialize()  // 単語データ読み込み
    Reviews.initialize() //復習テストを読み込み
    Answers.initialize() //回答データ読み込み

    install(Locations)

    install(ContentNegotiation) {
        gson {
            serializeNulls()
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
            excludeFieldsWithoutExposeAnnotation()
        }
    }

    install(CORS) {
        method(HttpMethod.Get)
        method(HttpMethod.Post)

        header("X-Requested-With")
        header("X-Token")

        allowCredentials = true

        val whitelist = mutableListOf<String>()

        if (ApplicationConfig.PRODUCTION) {
            whitelist.addAll(
                listOf(ApplicationConfig.FRONTEND_APP_DOMAIN)
            )
        } else {
            whitelist.addAll(
                listOf("localhost:5051")
            )
        }

        whitelist.forEach { addr -> host(addr, schemes = listOf("http", "https")) }
    }

    install(ForwardedHeaderSupport)

    install(DefaultHeaders) {
        header("X-Content-Type-Options", "SAMEORIGIN")
        header("X-Frame-Options", "SAMEORIGIN")
        header("X-XSS-Protection", "1; mode=block")
    }

    install(CallLogging) {
        level = Level.INFO
    }

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

        exception<Exception> { cause ->
            val stack = cause.stackTrace
            val message = "${cause.localizedMessage}#${stack[stack.size - 1]}"
            val hash = DigestUtils.sha512Hex(message).toUpperCase()

            if (context.request.header("X-Requested-With").equals("XMLHttpRequest")) {
                context.response.header("X-Server-Error-Hash", hash) //ハッシュを含む
                context.respond(ResponseInfo(result = 500, message = "サーバー側でエラーが発生しました"))
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


        }
    }

    install(Routing) {
        authentication()
        user()
        word()
        category()
        answers()
        reviews()
    }
}


data class ResponseInfo(val data: Any? = null, val result: Int = HttpStatusCode.OK.value, val message: String = "")


class AuthorizationException(error: String = "") : RuntimeException(error)
class NotFoundException(error: String = "") : RuntimeException(error)
class BadRequestException(error: String = "") : RuntimeException(error)
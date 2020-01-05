package com.aopro.wordlink

import com.aopro.wordlink.database.DatabaseHandler
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.request.header
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.*
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {

    DatabaseHandler.initialize() //データベース初期化


    install(Locations)

    install(CORS) {
        method(HttpMethod.Get)
        method(HttpMethod.Post)

        header("X-Requested-With")
        allowCredentials = true

        val whitelist = mutableListOf<String>()
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
            context.respondText(cause.localizedMessage, status = HttpStatusCode.Unauthorized)
        }

        exception<NotFoundException> { cause ->
            context.respondText(cause.localizedMessage, status = HttpStatusCode.NotFound)
        }

        exception<BadRequestException> { cause ->
            context.respondText(cause.localizedMessage, status = HttpStatusCode.BadRequest)
        }

        exception<Exception> { cause ->
            val stack = cause.stackTrace
            val message = "${cause.localizedMessage}#${stack[stack.size - 1]}"
            val hash = DigestUtils.sha512Hex(message).toUpperCase()

            if (context.request.header("X-Requested-With").equals("XMLHttpRequest")) {
                context.response.header("X-Server-Error-Hash", hash) //ハッシュを含む
                context.respondText("サーバー側でエラーが発生しました", status = HttpStatusCode.InternalServerError)
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

    routing {
        get("/") {
           context.respond("Hello World!!")
        }
    }
}


class AuthorizationException(error: String = "") : RuntimeException(error)
class NotFoundException(error: String = "") : RuntimeException(error)
class BadRequestException(error: String = "") : RuntimeException(error)
package com.aopro.wordlink.controller

import com.aopro.wordlink.*
import com.aopro.wordlink.database.model.User
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.fromBase64
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.annotations.Expose
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import java.time.LocalDateTime
import java.util.*


@Location("authentication")
class Authentication {

    @Location("credential")
    class Credential {
        data class Payload(
            @Expose val base64Email: String = "",
            @Expose val base64Password: String = "")
    }

    @Location("session")
    class Session {
    }

    @Location("logout")
    class Logout
}


fun Route.authentication() {
    post<Authentication.Credential> {
        val payload = context.receive(Authentication.Credential.Payload::class)
        requireNotNullAndNotEmpty(payload.base64Email, payload.base64Password)

        val requestEmail = payload.base64Email
        println(requestEmail)
        val target = Users.users().find { usr -> usr.username == requestEmail } ?: throw BadRequestException("ユーザーネーム、またはパスワードが間違っています。")

        if (isSamePassword(payload.base64Password, target.encryptedPassword)) {
            context.respond(ResponseInfo(data = target, message = generateAuthenticationToken(target)))
        } else throw BadRequestException("ユーザーネーム、またはパスワードが間違っています。")
    }

    get<Authentication.Logout> {
    }

    post<Authentication.Session> {

    }
}

fun generateAuthenticationToken(user: User): String {
    val expiration = LocalDateTime.now().plusMonths(1).atZone(DefaultZone)
    val algorithm = Algorithm.HMAC256(ApplicationConfig.JWT_SECRET)
    return JWT.create()
        .withExpiresAt(Date.from(expiration.toInstant()))
        .withClaim(User.Model::_id.name, user.id)
        .withClaim(User.Model::first_name.name, user.firstName)
        .withClaim(User.Model::last_name.name, user.lastName)
        .withClaim(User.Model::created_at.name, user.createdAt)
        .withClaim(User.Model::updated_at.name, user.updatedAt)
        .withClaim(User.Model::access_level.name, user.accessLevel)
        .sign(algorithm)
}


fun ApplicationRequest.tokenAuthentication(accessLevel: Int = 1): User {
    val token = header("X-Access-Token")
        ?: throw AuthorizationException("リクエスト形式が無効です")

    val algorithm = Algorithm.HMAC256(ApplicationConfig.JWT_SECRET)
    val jwt = try { JWT.require(algorithm).build().verify(token) }
    catch (e: Exception) {
        throw AuthorizationException("トークンが無効です。再読み込みをしてください.") }

    val id = jwt.getClaim(User.Model::_id.name).asString()
    val user = Users.users().find { user -> user.id == id } ?: throw AuthorizationException("トークンが無効です。再読み込みをしてください")


    if (user.accessLevel < accessLevel) throw AuthorizationException("アクセス権限がありません.")

    return user
}
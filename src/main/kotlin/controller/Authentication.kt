package com.aopro.wordlink.controller

import com.aopro.wordlink.ApplicationConfig
import com.aopro.wordlink.AuthorizationException
import com.aopro.wordlink.database.model.User
import com.aopro.wordlink.utilities.DefaultZone
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.date.toGMTDate
import java.lang.Exception
import java.time.LocalDateTime
import java.util.*


@Location("authentication")
class Authentication {

    @Location("credential")
    class Credential {
        data class Payload(val id: String = "", val password: String = "")
    }

    @Location("logout")
    class Logout
}


fun Route.authentication() {
    post<Authentication.Credential> {
        val payload = context.receive(Authentication.Credential.Payload::class)

        val user = Users.users().find { user -> user.id == payload.id } //same id

        if (user != null && isSamePassword(payload.password, user.encryptedPassword)) {
            context.response.cookies.append(
                name = "_SESSION",
                value = generateAuthenticationToken(user),
                expires = LocalDateTime.now().plusMonths(1).atZone(DefaultZone).toGMTDate(),
                httpOnly = true,
                path = "/",
                domain = if (ApplicationConfig.PRODUCTION) ApplicationConfig.BACKEND_APP_DOMAIN else "localhost"
            )
        }
    }

    post<Authentication.Logout> {

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
    val token = header("X-Token")
        ?.substring("Bearer ".toCharArray().size)
        ?: throw AuthorizationException("Request has been miss.")

    val algorithm = Algorithm.HMAC256(ApplicationConfig.JWT_SECRET)
    val jwt = try { JWT.require(algorithm).build().verify(token) }
    catch (e: Exception) { throw AuthorizationException("Token has been invalid.") }

    val id = jwt.getClaim(User.Model::_id.name).asString()
    val level = jwt.getClaim(User.Model::access_level.name).asInt()

    if (level < accessLevel) throw AuthorizationException("Your token doesnt have access this content.")

    return Users.users().find { user -> user.id == id } ?: throw AuthorizationException("Token has been invalid.")
}
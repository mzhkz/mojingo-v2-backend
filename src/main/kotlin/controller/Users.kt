package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.User
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.routing.Route
import org.apache.commons.codec.digest.DigestUtils
import org.litote.kmongo.getCollection
import java.util.*

object Users {

    private val users = mutableListOf<User>()
    private lateinit var session: MongoCollection<User.Model>

    fun users() = users.toMutableList()

    fun initialize() {
        session = DatabaseHandler
            .databaseSession
            .getCollection<User.Model>("users")

        users.addAll(session.find().map { model ->
            User(
                id = model._id,
                firstName = model.first_name,
                lastName = model.last_name,
                encryptedPassword = model.encrypted_password,
                accessLevel = model.access_level,
                createdAt = Date(model.created_at * 1000),
                updatedAt = Date(model.updated_at * 1000)
            )
        })
    }
}


@Location("/user")
class UserRoute {

}

fun Route.user() {

}

fun isSamePassword(original: String, encrypted: String) = DigestUtils.sha256Hex(original) == encrypted
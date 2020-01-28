package com.aopro.wordlink.controller

import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.User
import com.aopro.wordlink.utilities.DefaultZone
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.routing.Route
import org.apache.commons.codec.digest.DigestUtils
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.time.LocalDateTime
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
                username = model.username,
                firstName = model.first_name,
                lastName = model.last_name,
                encryptedPassword = model.encrypted_password,
                accessLevel = model.access_level,
                createdAt = Date(model.created_at * 1000),
                updatedAt = Date(model.updated_at * 1000)
            )
        })
    }

    /** データベースに記録ユーザーを登録する*/
    fun insertUser(user: User) {
        session.insertOne(User.Model(
            _id = user.id,
            username = user.username,
            first_name = user.firstName,
            last_name = user.lastName,
            created_at = user.createdAt.time,
            updated_at = user.updatedAt.time,
            access_level = user.accessLevel,
            encrypted_password = user.encryptedPassword
        ))
    }

    /** データベースのユーザーを更新する*/
    fun updateUser(vararg users: User) {
        users.forEach { usr ->
            session.updateOne(
                User.Model::_id eq usr.id,
                User.Model::username setTo usr.username,
                User.Model::encrypted_password setTo usr.encryptedPassword,
                User.Model::first_name setTo usr.firstName,
                User.Model::last_name setTo usr.lastName,
                User.Model::updated_at setTo Date
                    .from(
                        LocalDateTime
                            .now()
                            .atZone(DefaultZone)
                            .toInstant()).time
            )
        }
    }
}


@Location("/user")
class UserRoute {

}

fun Route.user() {

}

/** ハッシュ化したパスワードが一致するかどうか*/
fun isSamePassword(original: String, encrypted: String) = DigestUtils.sha256Hex(original) == encrypted
package com.aopro.wordlink.controller

import com.aopro.wordlink.*
import com.aopro.wordlink.database.DatabaseHandler
import com.aopro.wordlink.database.model.User
import com.aopro.wordlink.utilities.CurrentUnixTime
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.currentUnixTimediff
import com.aopro.wordlink.utilities.splitAsPagination
import com.google.gson.annotations.Expose
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
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
                createdAt = model.created_at,
                updatedAt = model.updated_at
            )
        })

        if (ApplicationConfig.ALLOWED_ROOT) {
            //Rootアカウント
            users.add(User(
                id = "system_root",
                username = "root",
                firstName = "System",
                lastName = "Administrator",
                createdAt = CurrentUnixTime,
                updatedAt = CurrentUnixTime,
                accessLevel = 2,
                encryptedPassword = encryptPassword(ApplicationConfig.ROOT_PASSWORD)
            ))
        }
    }

    /** データベースに記録ユーザーを登録する*/
    fun insertUser(user: User) {
        session.insertOne(User.Model(
            _id = user.id,
            username = user.username,
            first_name = user.firstName,
            last_name = user.lastName,
            created_at = user.createdAt,
            updated_at = user.updatedAt,
            access_level = user.accessLevel,
            encrypted_password = user.encryptedPassword
        ))
        users.add(user)
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
                User.Model::access_level setTo usr.accessLevel,
                User.Model::updated_at setTo CurrentUnixTime
            )
        }
    }
}


@Location("/user")
class UserRoute {

    @Location("/enroll")
    class Enroll {
        data class Payload(
            @Expose val username: String = "",
            @Expose val accessLevel: Int = 0,
            @Expose val firstName: String = "",
            @Expose val lastName: String = "",
            @Expose val password: String = ""
        )
    }

    @Location("/list")
    class List


    @Location("/profile/{id}")
    data class Profile(val id: String = "") {

        data class ProfileResponse(
            @Expose val profile: User? = null,
            @Expose val reviews: MutableList<ReviewRoute.List.ReviewResponse> = mutableListOf()
        )

        @Location("/reset-pass")
        class ResetPassword {

            data class Payload(
                @Expose val password: String = ""
            )
        }

        @Location("/update")
        class Update {

            data class Payload(
                @Expose val firstName: String = "",
                @Expose val lastName: String = "",
                @Expose val username: String = ""
            )
        }

        @Location("/qualify")
        class Qualify {
            data class Payload(
                @Expose val applyLevel: Int = 0
            )
        }
    }
}

fun Route.user() {

    post <UserRoute.Enroll>{
        val authUser = context.request.tokenAuthentication(2)
        val payload = context.receive(UserRoute.Enroll.Payload::class)

        val user = User(
           id = UUID.randomUUID().toString().replace("-", ""),
            username = payload.username,
            firstName = payload.firstName,
            lastName = payload.lastName,
            encryptedPassword = encryptPassword(payload.password),
            accessLevel = payload.accessLevel,
            createdAt = CurrentUnixTime,
            updatedAt = CurrentUnixTime
        )

        Users.insertUser(user) //DBに登録

        context.respond(ResponseInfo(data = user))
    }

    get<UserRoute.Profile> { query ->
        val authUser = context.request.tokenAuthentication()

        val targetId = context.parameters["id"]
        val id =
            if (targetId == "me" || targetId == authUser.id)
                authUser.id
            else {
                if (authUser.accessLevel >= 2)
                    targetId
                else throw AuthorizationException("Your token doesn't access this content.")
            }

        val targetProfile = Users.users().find { user -> user.id == id } ?: throw BadRequestException("Not found '$targetId' as User.")
        val targetReviews = Reviews.reviews()
            .filter { review -> review.owner.id == targetProfile.id }.toMutableList()

        context.respond(ResponseInfo(
            data = UserRoute.Profile.ProfileResponse(
                profile = targetProfile,
                reviews = targetReviews.splitAsPagination(1, 5).map { review ->

                    val impacts = Answers.answers().mapNotNull { answer -> answer.histories.find { history ->  history.impactReviewId == review.id}}

                    ReviewRoute.List.ReviewResponse(
                        review = review,
                        correctSize = impacts.count { history -> history.result == 1 } ,
                        incorrectSize = impacts.count { history -> history.result == 0 },
                        createAgo = review.createdAt.currentUnixTimediff()
                    )
                }.toMutableList()
            )
            , message = "successful"))
    }

    post<UserRoute.Profile.Update> {
        val authUser = context.request.tokenAuthentication(2)
        val payload = context.receive(UserRoute.Profile.Update.Payload::class)

        requireNotNullAndNotEmpty(payload.firstName, payload.lastName, payload.username)

        val targetId = context.parameters["id"]
        val target = Users.users().find { user -> user.id == targetId } ?: throw BadRequestException("ユーザが見つかりません")

        target.apply {
            username = payload.username
            firstName = payload.firstName
            lastName = payload.lastName
            updatedAt = CurrentUnixTime
        }

        Users.updateUser(target)
        context.respond(ResponseInfo(data = target, message = "successful"))
    }

    post<UserRoute.Profile.Qualify> {
        val authUser = context.request.tokenAuthentication(2)
        val payload = context.receive(UserRoute.Profile.Qualify.Payload::class)
        requireNotNullAndNotEmpty(payload.applyLevel)

        val targetId = context.parameters["id"]
        val target = Users.users().find { user -> user.id == targetId } ?: throw BadRequestException("ユーザが見つかりません")

        target.apply {
            accessLevel = payload.applyLevel
            updatedAt = CurrentUnixTime
        }

        Users.updateUser(target)
        context.respond(ResponseInfo(data = target, message = "successful"))
    }

    post<UserRoute.Profile.ResetPassword> {
        val authUser = context.request.tokenAuthentication(2)
        val payload = context.receive(UserRoute.Profile.ResetPassword.Payload::class)
        requireNotNullAndNotEmpty(payload.password)

        val targetId = context.parameters["id"]
        val target = Users.users().find { user -> user.id == targetId } ?: throw BadRequestException("ユーザが見つかりません")

        target.apply {
            encryptedPassword = encryptPassword(payload.password);
            updatedAt = CurrentUnixTime
        }

        Users.updateUser(target)
        context.respond(ResponseInfo(data = target, message = "successful"))
    }


    get<UserRoute.List >{
        val authUser = context.request.tokenAuthentication(2)
        context.respond(ResponseInfo(data = Users.users()))
    }
}

/** ハッシュ化したパスワードが一致するかどうか*/
fun isSamePassword(original: String, encrypted: String) = encryptPassword(original) == encrypted

fun encryptPassword(original: String) =  DigestUtils.sha256Hex(original)
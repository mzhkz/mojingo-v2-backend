package com.aopro.wordlink.controller

import com.aopro.wordlink.database.model.User
import com.mongodb.client.MongoCollection
import io.ktor.locations.Location
import io.ktor.routing.Route

object Users {

    private val users = mutableListOf<User>()
    private lateinit var collectionSession: MongoCollection<User.Model>


}


@Location("/user")
class UserRoute {

}

fun Route.user() {

}
package com.aopro.wordlink.database.model

import java.util.*

class User(val id: String,
           val firstName: String,
           val lastName: String,
           val accessLevel: Int = 0,
           val createdAt: Date) {

    data class Model(
        val _id: String = "",
        val first_name: String = "",
        val last_name: String = "",
        val access_level: Int = 0,
        val created_at: Long = 0L
    )
}
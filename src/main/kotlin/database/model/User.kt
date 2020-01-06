package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

class User(@Expose val id: String,
           val encryptedPassword: String,
           @Expose val firstName: String,
           @Expose val lastName: String,
           @Expose val accessLevel: Int = 0,
           @Expose val createdAt: Date,
           @Expose val updatedAt: Date) {

    data class Model(
        val _id: String = "",
        val encrypted_password: String = "",
        val first_name: String = "",
        val last_name: String = "",
        val access_level: Int = 0,
        val created_at: Long = 0L,
        val updated_at: Long = 0L
    )
}
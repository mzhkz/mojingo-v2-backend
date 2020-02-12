package com.aopro.wordlink.database.model

import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class User(@Expose val id: String,
           @Expose val username: String,
           val encryptedPassword: String,
           @Expose val firstName: String,
           @Expose val lastName: String,
           @Expose val accessLevel: Int = 0,
           @Expose val createdAt: Date,
           @Expose val updatedAt: Date) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            User(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                username = "NOT_EXIST_USER",
                firstName = "not",
                lastName = "exist",
                createdAt = Date(0L),
                updatedAt = Date(0L),
                accessLevel = -1,
                encryptedPassword = ""
            )
    }

    data class Model(
        val _id: String = "",
        val username: String = "",
        val encrypted_password: String = "",
        val first_name: String = "",
        val last_name: String = "",
        val access_level: Int = 0,
        val created_at: Long = 0L,
        val updated_at: Long = 0L
    )
}
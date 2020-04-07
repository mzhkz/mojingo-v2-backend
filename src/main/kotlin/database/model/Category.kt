package me.mojingo.v2.backend.database.model

import me.mojingo.v2.backend.utilities.randomBytes
import com.google.gson.annotations.Expose
import me.mojingo.v2.backend.controller.Users
import org.apache.commons.codec.digest.DigestUtils
class Category(@Expose val id: String,
               @Expose var name: String,
               val spreadSheetId: String,
               @Expose var owner: User,
               @Expose var description: String,
               @Expose val createdAt: Long,
               @Expose val updatedAt: Long,
               @Expose val shareUsers: MutableList<User>,
               @Expose val private: Boolean) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Category(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                name = "NOT_EXIST_CATEGORY",
                spreadSheetId = "NONE",
                description = "",
                owner = User.notExistObject(),
                createdAt = 0L,
                updatedAt = 0L,
                shareUsers = mutableListOf(),
                private = true
            )
    }

    data class Model(
        val _id: String = "",
        val name: String = "",
        val spread_sheet_id: String,
        val description: String = "",
        val owner_id: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val share_users: MutableList<String> = mutableListOf(),
        val private: Boolean = false
    )
}


package me.mojingo.v2.backend.database.model

import me.mojingo.v2.backend.controller.Users
import me.mojingo.v2.backend.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Review(
    @Expose val id: String,
    @Expose val name: String,
    @Expose val description: String,
    @Expose val owner: User,
    @Expose val entries: MutableList<Word>,
    @Expose val answers: MutableList<Answer>,
    @Expose var finished: Boolean, //問題の間違えの確認など、すべてのタスクが完了したかどうか。
    @Expose val createdAt: Long,
    @Expose val updatedAt: Long) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Review(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                name = "NOT_EXIST_CATEGORY",
                entries = mutableListOf(),
                answers = mutableListOf(),
                finished = false,
                description = "",
                owner = User.notExistObject(),
                createdAt = 0L,
                updatedAt = 0L
            )
    }


    data class Model(
        val _id: String = "",
        val name: String = "",
        val description: String = "",
        val owner_id: String  = "",
        val entries: MutableList<String>,
        val answers: MutableList<String>,
        val finished: Boolean = false,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L
    )
}
package com.aopro.wordlink.database.model

import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Answer(
    @Expose val id: String,
    @Expose val user: User,
    @Expose val word: Word,
    @Expose val createdAt: Long,
    @Expose val updatedAt: Long,
    @Expose val histories: MutableList<History>) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Answer(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                user = User.notExistObject(),
                word = Word.notExistObject(),
                createdAt = 0L,
                updatedAt = 0L,
                histories = mutableListOf()
            )
    }

    data class Model(
        val _id: String = "",
        val userId: String = "",
        val word_name: String = "",
        val category_id: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val histories: MutableList<History.Model> = mutableListOf()
    )

    class History(
        @Expose val impactReviewId: String,
        @Expose val result: Int,
        @Expose val postAt: Long) {

        data class Model(
            val impact_review: String,
            val result: Int = 0,
            val post_at: Long = 0L
        )
    }
}
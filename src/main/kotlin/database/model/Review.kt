package com.aopro.wordlink.database.model

import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Review(
    @Expose val id: String,
    @Expose val name: String,
    @Expose val description: String,
    @Expose val entries: MutableList<Word>,
    @Expose val answers: MutableList<Answer>,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Review(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                name = "NOT_EXIST_CATEGORY",
                entries = mutableListOf(),
                answers = mutableListOf(),
                description = "",
                createdAt = Date(0L),
                updatedAt = Date(0L)
            )
    }


    data class Model(
        val _id: String = "",
        val name: String = "",
        val description: String = "",
        val entries: MutableList<String>,
        val answers: MutableList<String>,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L
    )
}
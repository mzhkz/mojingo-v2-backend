package com.aopro.wordlink.database.model

import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Word(
    @Expose val id: String,
    @Expose val number: Int,
    @Expose var name: String,
    @Expose var mean: String,
    @Expose val category: Category,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Word(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                number = -1, //number of category
                name = "NOT_EXIST_WORD",
                mean = "",
                category = Category.notExistObject(),
                createdAt = Date(0L),
                updatedAt = Date(0L)
            )
    }

    data class Model(
        val _id: String = "",
        val name: String = "",
        val mean: String = "",
        val category_id: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L
    )
}
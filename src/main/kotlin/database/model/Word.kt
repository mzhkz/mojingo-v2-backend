package com.aopro.wordlink.database.model

import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class Word(
    @Expose val number: Int,
    @Expose var name: String,
    @Expose var mean: String,
    @Expose var description: String,
    @Expose val category: Category) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Word(
                number = -1, //number of category
                name = "NOT_EXIST_WORD",
                mean = "",
                category = Category.notExistObject(),
                description = ""
            )
    }

    data class Model(
        val name: String = "",
        val mean: String = "",
        val description: String = "",
        val category_id: String = ""
    )
}
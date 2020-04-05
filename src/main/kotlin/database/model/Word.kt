package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose

class Word(
    @Expose var number: Int,
    @Expose var name: String,
    @Expose var mean: String,
    @Expose var description: String,
    @Expose var category: Category) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Word(
                number = -1, //number of category
                name = "-- 存在しない単語 --",
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
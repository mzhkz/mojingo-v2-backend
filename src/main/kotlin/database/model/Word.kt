package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

class Word(
    @Expose val id: String,
    @Expose val name: String,
    @Expose val mean: String,
    @Expose private val categoryId: String,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date,
    @Expose private val lastEditedId: String) {

    data class Model(
        val _id: String = "",
        val name: String = "",
        val mean: String = "",
        val category_id: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val last_edit_id: String = ""
    )
}
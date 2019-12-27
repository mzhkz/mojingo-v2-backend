package com.aopro.wordlink.database.model

import java.util.*

class Word(
    val id: String,
    val name: String,
    val mean: String,
    private val categoryId: String,
    val createdAt: Date,
    val updatedAt: Date,
    private val lastEditedId: String) {

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
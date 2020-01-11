package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

class Review(
    @Expose val id: String,
    @Expose val name: String,
    @Expose val description: String,
    @Expose val entries: MutableList<Word>,
    @Expose val answers: MutableList<Answer>,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date) {


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
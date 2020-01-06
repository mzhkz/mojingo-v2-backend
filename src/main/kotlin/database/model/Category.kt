package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

class Category(@Expose val id: String,
               @Expose val name: String,
               @Expose val description: String,
               @Expose val createdAt: Date,
               @Expose val updatedAt: Date,
               @Expose val private: Boolean) {

    data class Model(
        val _id: String = "",
        val name: String = "",
        val description: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val private: Boolean = false
    )
}
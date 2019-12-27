package com.aopro.wordlink.database.model

import java.util.*

class Category(val id: String,
               val name: String,
               val description: String,
               val createdAt: Date,
               val updatedAt: Date,
               val private: Boolean) {

    data class Model(
        val _id: String = "",
        val name: String = "",
        val description: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val private: Boolean = false
    )
}
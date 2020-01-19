package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

class Answer(
    @Expose val id: String,
    @Expose val user: User,
    @Expose val word: Word,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date,
    @Expose val histories: MutableList<History>) {

    data class Model(
        val _id: String = "",
        val userId: String = "",
        val wordId: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val histories: MutableList<History.Model> = mutableListOf()
    )

    class History(
        @Expose val impactReview: Review,
        @Expose val result: Int,
        @Expose val postAt: Date) {

        data class Model(
            val impact_review: String,
            val result: Int = 0,
            val post_at: Long = 0L
        )
    }
}
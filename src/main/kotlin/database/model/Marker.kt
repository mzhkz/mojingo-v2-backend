package com.aopro.wordlink.database.model

import com.google.gson.annotations.Expose
import java.util.*

/** 回答をレビューに記録するための、一時的なデータモデル */
class Marker(
    @Expose val id: String, //Session id
    @Expose val description: String,
    @Expose val entries: MutableList<Word>,
    @Expose val answers: MutableList<Word>,
    @Expose val reflectReview: Review,
    @Expose val createdAt: Date,
    @Expose val updatedAt: Date
)
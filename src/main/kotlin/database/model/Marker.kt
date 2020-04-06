package me.mojingo.v2.backend.database.model

import com.google.gson.annotations.Expose
import java.util.*

/** 回答をレビューに記録するための、一時的なデータモデル */
class Marker(
    @Expose val id: String, //Session id
    @Expose val reflectReview: Review,
    @Expose val createdAt: Long,
    @Expose val updatedAt: Long,
    var correctsCheck: String, //正解したことを証明する暗号
    var incorrectCheck: String //間違えたことを証明する暗号


)
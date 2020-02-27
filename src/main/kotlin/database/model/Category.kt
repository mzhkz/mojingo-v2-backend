package com.aopro.wordlink.database.model

import com.aopro.wordlink.controller.Words
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.time.LocalDateTime
import java.util.*

class Category(@Expose val id: String,
               @Expose val name: String,
               @Expose val description: String,
               @Expose val createdAt: Date,
               @Expose val updatedAt: Date,
               @Expose val private: Boolean) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            Category(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                name = "NOT_EXIST_CATEGORY",
                description = "",
                createdAt = Date(0L),
                updatedAt = Date(0L),
                private = true
            )
    }

    data class Model(
        val _id: String = "",
        val name: String = "",
        val description: String = "",
        val created_at: Long = 0L,
        val updated_at: Long = 0L,
        val private: Boolean = false
    )
}


/** CSVファイルから単語を読み込む*/
fun readWordCSV(line: MutableList<String>, category: Category): MutableList<Word> {
//    val regex = Regex("^([\\d]+)(,)([\\w\\d\\s]+)(,)([\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\p{InBasicLatin}\\w\\d\\s]+)\$") //No,Name,Mean
    val regex = Regex("^(No)(,)(Name)(,)(Mean)\$") //No,Name,Mean
    var assignNumber = 0

    return line.mapNotNull { str ->
        val match = regex.matchEntire(str) //タイトルかどうか
        if (match == null && str.indexOf(",") > 0) {
            val contants = str.split(",")
            assignNumber+=1
            Word(
                id = Words.generateId(),
                number = assignNumber,
                name = contants[1],
                mean = contants[2],
                category = category,
                createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
                updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
            )
        } else null
    }.toMutableList()

}
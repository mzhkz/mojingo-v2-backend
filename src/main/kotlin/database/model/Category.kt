package com.aopro.wordlink.database.model

import com.aopro.wordlink.controller.Categories
import com.aopro.wordlink.controller.Words
import com.aopro.wordlink.utilities.DefaultZone
import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.charset.Charset
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
    val regex = Regex("^(¥¥d),(¥¥w+),(¥¥w+)") //No,Name,Mean

    return line.mapNotNull { str ->
        val match = regex.matchEntire(str)
        if (match != null) {
          Word(
              id = Words.generateId(),
              name = match.groupValues[1],
              mean = match.groupValues[2],
              category = category,
              createdAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant()),
              updatedAt = Date.from(LocalDateTime.now().atZone(DefaultZone).toInstant())
          )
        } else null
    }.toMutableList()

}
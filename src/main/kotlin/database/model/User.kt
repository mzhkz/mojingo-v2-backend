package com.aopro.wordlink.database.model

import com.aopro.wordlink.controller.Answers
import com.aopro.wordlink.controller.Categories
import com.aopro.wordlink.controller.Words
import com.aopro.wordlink.utilities.CurrentUnixTime
import com.aopro.wordlink.utilities.randomBytes
import com.google.gson.annotations.Expose
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class User(@Expose val id: String,
           @Expose var username: String,
           var encryptedPassword: String,
           @Expose var firstName: String,
           @Expose var lastName: String,
           @Expose var accessLevel: Int = 0,
           @Expose val createdAt: Long,
           @Expose var updatedAt: Long,
           var cacheRecommended: MutableList<Recommended> = mutableListOf(),
           private var cacheRecommendedRefreshDate: Long = 0) {

    companion object {
        @JvmStatic
        fun notExistObject() =
            User(
                id = "not_exist_${DigestUtils.shaHex(randomBytes()).substring(0,7)}",
                username = "NOT_EXIST_USER",
                firstName = "not",
                lastName = "exist",
                createdAt = 0L,
                updatedAt = 0L,
                accessLevel = -1,
                encryptedPassword = ""
            )
    }

    /** キャッシュデータを再取得*/
    fun refreshRecommended() {
       if (CurrentUnixTime - cacheRecommendedRefreshDate > 60 * 30) { //30 min delay
           val recommended = Answers.pickupRecommended(this).toMutableList()

           cacheRecommended = Categories.categories().map { category ->
               Recommended(
                   category,
                   recommended.filter { word -> word.category.id == category.id }.toMutableList()
               )
           }.toMutableList()
           cacheRecommendedRefreshDate = CurrentUnixTime
       }
    }

    /** 回答記録からキャッシュデータを更新*/
    fun refreshLastAnswered(category: Category, answer: Answer) {
        cacheRecommended.find { it.category.id == category.id }
            ?.entries?.removeIf { it.id == answer.word.id }//回答済みの単語を削除する
    }


    data class Recommended(
        val category: Category,
        val entries: MutableList<Word>
    )

    data class Model(
        val _id: String = "",
        val username: String = "",
        val encrypted_password: String = "",
        val first_name: String = "",
        val last_name: String = "",
        val access_level: Int = 0,
        val created_at: Long = 0L,
        val updated_at: Long = 0L
    )
}
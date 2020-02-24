package com.aopro.wordlink.utilities

import org.apache.commons.codec.digest.DigestUtils
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/** StringからDateに変換 */
fun String.toDate(dateFormat: SimpleDateFormat = DefaultDateFormat): Date {
    return dateFormat.parse(this)
}

/**
 * Base64にエンコードします。
 * @param string エンコードする文字列
 */
fun String.toBase64(): String =
        Base64.getEncoder().encodeToString(this.toByteArray(Charset.forName("UTF-8")))

/**
 * Base64にデコードします。
 * @param string デコードする文字列
 */
fun String.fromBase64(): String =
        String(Base64.getDecoder().decode(this))

fun String.toURLEncode(): String =
        URLEncoder.encode(this, "UTF-8")

fun String.toURLDecode(): String =
        URLEncoder.encode(this, "UTF-8")

/** StringからURIに変換 */
fun String.toURI(): URI {
    return URI.create(this)
}

/** 長い文字を省略して表示*/
fun String.toShortDecoration(max: Int, etc: String = "..."): String {
    return if (length <= max) this else substring(0, max) + etc
}

val ensureIdElemments by lazy {
    var c = 'A'
    val mutableList = mutableListOf<String>()
    while (c <= 'Z') {
        mutableList.add(c.toString()) //大文字
        mutableList.add(c.toString().toLowerCase()) //小文字
        ++c
    }
    mutableList.plus((1..9).map { it.toString() })
}

fun randomBytes(): ByteArray {
    val random = ByteArray(128)
    Random().nextBytes(random) //CentOS
    return random
}

/** SHA-512ランダム生成*/
val generateRandomSHA512: String
    get() = DigestUtils.sha512Hex(randomBytes())



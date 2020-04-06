package me.mojingo.v2.backend.utilities

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/** 変換に用いるフォーマット*/
val DefaultDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
val AboutDateFormat = SimpleDateFormat("MM月dd日")
val DefaultZone = ZoneId.of("Asia/Tokyo")!!

/** タイムゾーンのAsia/Tokyoの現在のUNIXTIMEを取得する*/
val CurrentUnixTime: Long
    get() = ZonedDateTime.now(DefaultZone).toEpochSecond()

/** Dateから文字列に変換*/
fun Date.toFormatString(dataFormat: SimpleDateFormat = DefaultDateFormat): String {
    return dataFormat.format(this)
}

fun Long.currentUnixTimediff(): String {

    val padZero: (Int) -> String = { value ->
        if (value < 10)
            "0$value"
        else
            value.toString()
    }

    val now = CurrentUnixTime //現在時刻
    val diff = (now - this).toDouble() //時間差

    val years = Math.floor(diff / (3600.0 * 24.0 * 365.0)).toInt()
    val months = Math.floor(diff % (3600.0 * 24.0 * 365.0) / (3600.0 * 24.0 * 30)).toInt()
    val weeks = Math.floor(diff % (3600.0 * 24.0 * 30) / (3600.0 * 24.0 * 7)).toInt()
    val days = Math.floor(diff % (3600.0 * 24.0 * 30) / (3600.0 * 24.0)).toInt()
    val hours = Math.floor(diff % (3600.0 * 24.0) / 3600.0).toInt()
    val minutes = Math.floor(diff % 3600.0 / 60.0).toInt()
    val seconds = Math.floor(diff % 60.0).toInt()

    return when  {
        years > 0.0 -> "${years}年前"
        months > 0.0 -> "${months}ヵ月前"
        weeks > 0.0 -> "${weeks}週間前"
        days > 0.0 -> "${days}日前"
        hours > 0.0 -> "${hours}時間前"
        minutes > 0.0 -> "${minutes}分前"
        else -> {
           when {
               seconds <= 30 -> "たった今"
               else ->"${seconds}秒前"
           }
        }
    }
}

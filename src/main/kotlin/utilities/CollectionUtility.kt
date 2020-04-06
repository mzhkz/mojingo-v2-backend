package me.mojingo.v2.backend.utilities


/**
 * ページネーションみたいなの使えそう
 * @param page ページ番号
 * @param index 何件取得するか
 */

fun <T> List<T>.splitAsPagination(page: Int = 1, index: Int = 1): List<T> {
    val pageNumber = page - 1
    val under: Int = if (pageNumber == 0) 0 else index * pageNumber
    val tOver: Int = if (pageNumber == 0) index else index * page
    val over: Int = if (tOver > size) size else tOver
    if (under >= over)
        return mutableListOf()
    return this.subList(under, over)
}

fun <T> List<T>.maximumAsPagination(index: Int): Int {
    var maxPage = Math.floor(size.toDouble() / index.toDouble()).toInt()
    if (size % index != 0) {
        maxPage += 1
    }
    return maxPage
}

fun List<*>.toArrayList() = ArrayList(this)



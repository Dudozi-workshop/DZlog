package com.dudoziworkshop.dzlog.domain.naming

internal fun splitFileName(displayName: String): Pair<String, String> {
    val dot = displayName.lastIndexOf('.')
    if (dot <= 0 || dot == displayName.lastIndex) return displayName to ""
    return displayName.take(dot) to displayName.drop(dot)
}

internal fun duplicateFileName(displayName: String, number: Int): String {
    val (stem, extension) = splitFileName(displayName)
    return "$stem ($number)$extension"
}

package com.example.dzlog.ui.table.rotating

fun digitsOnly(input: String): String = input.filter { it.isDigit() }

fun parsePositiveIntOrNull(digits: String, min: Int = 1): Int? {
    val n = digits.toIntOrNull() ?: return null
    return n.coerceAtLeast(min)
}

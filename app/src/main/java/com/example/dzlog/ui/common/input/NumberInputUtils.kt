package com.example.dzlog.ui.common.input

fun digitsOnly(input: String): String = input.filter { it.isDigit() }

fun parsePositiveIntOrNull(digits: String, min: Int = 1): Int? {
    val n = digits.toIntOrNull() ?: return null
    return n.coerceAtLeast(min)
}

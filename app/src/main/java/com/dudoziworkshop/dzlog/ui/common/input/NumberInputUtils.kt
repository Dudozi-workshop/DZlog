package com.dudoziworkshop.dzlog.ui.common.input

fun digitsOnly(input: String): String = input.filter { it.isDigit() }

/** Accept incomplete signed decimals without rounding or dropping trailing zeroes. */
fun decimalInputOrPrevious(input: String, previous: String): String {
    val normalized = input.replace('−', '-')
    return if (normalized.matches(Regex("-?[0-9]*(\\.[0-9]*)?"))) normalized else previous
}

fun parsePositiveIntOrNull(digits: String, min: Int = 1): Int? {
    val n = digits.toIntOrNull() ?: return null
    return n.coerceAtLeast(min)
}

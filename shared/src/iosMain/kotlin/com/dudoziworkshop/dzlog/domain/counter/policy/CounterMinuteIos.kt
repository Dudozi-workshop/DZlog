package com.dudoziworkshop.dzlog.domain.counter.policy

import platform.Foundation.*

internal actual fun formatCounterMinute(hour: Int, minute: Int): String {
    val formatter = NSNumberFormatter()
    formatter.locale = NSLocale.currentLocale
    formatter.numberStyle = NSNumberFormatterDecimalStyle
    formatter.minimumIntegerDigits = 2uL
    formatter.maximumFractionDigits = 0uL
    formatter.usesGroupingSeparator = false
    return requireNotNull(formatter.stringFromNumber(NSNumber(int = hour))) +
        requireNotNull(formatter.stringFromNumber(NSNumber(int = minute)))
}

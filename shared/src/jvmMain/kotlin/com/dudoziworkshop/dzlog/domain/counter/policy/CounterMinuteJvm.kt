package com.dudoziworkshop.dzlog.domain.counter.policy

internal actual fun formatCounterMinute(hour: Int, minute: Int): String = "%02d%02d".format(hour, minute)

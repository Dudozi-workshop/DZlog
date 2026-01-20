package com.example.dzlog.data.counter

import android.content.Context

interface CounterSync {
    fun syncNextCounter(
        context: Context,
        relativePath: String,
        counterDigits: Int,
        fnDelim: String,
        onResult: (nextCounter: Int) -> Unit,
        onFail: (String) -> Unit
    )
}

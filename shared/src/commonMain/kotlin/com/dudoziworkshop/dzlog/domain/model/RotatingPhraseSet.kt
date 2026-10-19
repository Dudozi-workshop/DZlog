package com.dudoziworkshop.dzlog.domain.model

enum class RotatingCounterProgressMode {
    PER_PHRASE,
    CONTINUOUS,
}

data class RotatingPhraseSet(
    val id: String,
    val name: String,
    val items: List<String>,
    val defaultEvery: Int = 1,
    val counterProgressMode: RotatingCounterProgressMode = RotatingCounterProgressMode.PER_PHRASE,
)

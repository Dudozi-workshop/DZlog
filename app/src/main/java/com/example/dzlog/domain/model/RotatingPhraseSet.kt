package com.example.dzlog.domain.model

data class RotatingPhraseSet(
    val id: String,
    val name: String,
    val items: List<String>,
    val defaultEvery: Int = 1
)

package com.dudoziworkshop.dzlog.domain.counter

data class CounterScopeOptions(
    val dateScopeValues: List<String> = emptyList(),
    val timeScopeValues: List<String> = emptyList(),
)

package com.dudoziworkshop.dzlog.feature.table.model

data class TableStyleState(
    val bgStyle: Int = 0,
    val gridEnabled: Boolean = true,
    val textColorMode: Int = 0,
    val manualTextColor: Int = 1,
    val valueScale: Int = 100,
    val textAlign: Int = 1,
    val bgAlpha: Int = 80,
)

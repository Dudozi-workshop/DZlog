package com.dudoziworkshop.dzlog.qa

internal class PhotoTable(text: String) {
    private val grid = text.trimEnd('\r', '\n').lines().map { it.split('|') }
    val rows = grid.size
    val cols = grid.first().size
    val labels = grid.flatten()

    init {
        require(grid.all { it.size == cols }) { "Inconsistent fixture row widths" }
    }
}

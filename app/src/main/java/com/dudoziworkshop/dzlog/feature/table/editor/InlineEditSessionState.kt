package com.dudoziworkshop.dzlog.feature.table.editor

data class InlineEditSessionState(
    val activeCellId: String? = null,
    val hasPushedUndoSnapshot: Boolean = false,
)

fun InlineEditSessionState.clearInlineSession(): InlineEditSessionState = InlineEditSessionState()

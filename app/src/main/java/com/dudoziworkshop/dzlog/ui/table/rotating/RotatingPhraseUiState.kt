package com.dudoziworkshop.dzlog.ui.table.rotating

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

data class RotatingPhraseUiState(
    val isTemplateDialogOpen: Boolean = false,
    val templateDialogCellId: String? = null,
    val templateDialogRestore: TableTemplateState? = null,
    val isCreateSetDialogOpen: Boolean = false,
    val createSetName: String = "",
    val pendingDeleteSetId: String? = null,
    val isSetEditDialogOpen: Boolean = false,
    val editingSetId: String? = null
)

package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = remember(templateState) { TableDetailViewModel(templateState) }
    var screenTemplate by remember(templateState) { mutableStateOf(templateState) }

    LaunchedEffect(templateState) {
        screenTemplate = templateState
        viewModel.applyTemplateFromUi(templateState, isActionCommit = false)
    }

    TableEditorScreen(
        templateState = screenTemplate,
        onTemplateChange = { updated ->
            viewModel.applyTemplateFromUi(updated, isActionCommit = true)
            screenTemplate = updated
            onTemplateChange(updated)
        },
        onReset = {
            onReset()
            viewModel.dispatch(TableDetailAction.Save)
        },
        onBack = {
            viewModel.dispatch(TableDetailAction.Save)
            onBack()
        },
    )
}

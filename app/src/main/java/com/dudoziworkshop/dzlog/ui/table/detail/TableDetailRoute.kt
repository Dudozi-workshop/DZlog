package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

@Composable
fun TableDetailRoute(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = rememberTableDetailViewModel(templateState)
    val viewState by viewModel.viewState

    TableDetailScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onReset = onReset,
        onBack = onBack,
        detailViewState = viewState,
    )
}

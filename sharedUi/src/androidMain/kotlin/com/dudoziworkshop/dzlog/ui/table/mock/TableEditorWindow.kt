package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.isImeVisible

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun tableEditorImeVisible(): Boolean = WindowInsets.isImeVisible

actual fun tableEditorDialogProperties(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

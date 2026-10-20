package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun tableEditorImeVisible(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

actual fun tableEditorDialogProperties(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false)

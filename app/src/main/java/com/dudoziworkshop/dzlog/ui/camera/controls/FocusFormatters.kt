package com.dudoziworkshop.dzlog.ui.camera.controls

import java.util.Locale

internal fun formatFocusUiValue(value: Float): String {
    return String.format(Locale.US, "%.1f", value.coerceIn(0f, 1f))
}

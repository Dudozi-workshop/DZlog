package com.dudoziworkshop.dzlog.ui.camera.controls

internal fun formatZoomActualLabel(zoomTenths: Int): String {
    val normalized = zoomTenths.coerceIn(10, 100)
    val whole = normalized / 10
    val tenth = normalized % 10
    return if (tenth == 0) {
        "${whole}x"
    } else {
        "${whole}.${tenth}x"
    }
}

internal fun formatZoomMenuBucketLabel(zoomTenths: Int): String {
    val normalized = zoomTenths.coerceIn(10, 100)
    return "${normalized / 10}x"
}

package com.dudoziworkshop.dzlog.ui.camera.controls

const val CAMERA_MIN_ZOOM_TENTHS = 10
const val CAMERA_MAX_ZOOM_TENTHS = 100

enum class ZoomTickWeight { MINOR, HALF, MAJOR }

fun cameraZoomTickWeight(tenths: Int): ZoomTickWeight = when {
    tenths % 10 == 0 -> ZoomTickWeight.MAJOR
    tenths % 5 == 0 -> ZoomTickWeight.HALF
    else -> ZoomTickWeight.MINOR
}

fun cameraZoomTickLabel(tenths: Int): String? =
    if (tenths >= CAMERA_MIN_ZOOM_TENTHS && tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        null
    }

/** Presets are derived from the device maximum and never duplicate its final stop. */
fun cameraZoomPresets(maxSupportedTenths: Int): List<Int> {
    val end = maxSupportedTenths.coerceIn(CAMERA_MIN_ZOOM_TENTHS, CAMERA_MAX_ZOOM_TENTHS)
    return (listOf(10, 20, 40, 100).filter { it <= end } + end).distinct().sorted()
}

fun cameraZoomNeighbors(currentTenths: Int, maxSupportedTenths: Int): Pair<Int?, Int?> {
    val normalized = currentTenths.coerceIn(
        CAMERA_MIN_ZOOM_TENTHS,
        maxSupportedTenths.coerceIn(CAMERA_MIN_ZOOM_TENTHS, CAMERA_MAX_ZOOM_TENTHS),
    )
    val presets = cameraZoomPresets(maxSupportedTenths)
    return presets.lastOrNull { it < normalized } to presets.firstOrNull { it > normalized }
}

package com.dudoziworkshop.dzlog.data.media

import kotlin.math.abs

enum class SavedGallerySwipeAction { NONE, PREVIOUS, NEXT, INFO }

/** Fitted one-finger navigation only. Zoom/pinch always owns its whole gesture. */
fun savedGallerySwipeAction(
    dx: Float, dy: Float, startScale: Float, multiplePointers: Boolean, threshold: Float,
): SavedGallerySwipeAction {
    if (multiplePointers || !startScale.isFinite() || startScale != 1f ||
        !dx.isFinite() || !dy.isFinite() || !threshold.isFinite() || threshold <= 0f) {
        return SavedGallerySwipeAction.NONE
    }
    return when {
        abs(dx) >= threshold && abs(dx) > abs(dy) ->
            if (dx < 0f) SavedGallerySwipeAction.NEXT else SavedGallerySwipeAction.PREVIOUS
        dy <= -threshold && abs(dy) > abs(dx) * 1.25f -> SavedGallerySwipeAction.INFO
        else -> SavedGallerySwipeAction.NONE
    }
}

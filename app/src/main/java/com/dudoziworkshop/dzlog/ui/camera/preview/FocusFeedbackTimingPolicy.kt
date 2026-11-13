package com.dudoziworkshop.dzlog.ui.camera.preview

/**
 * Timing rules for the camera's temporary focus controls.
 *
 * - A completed AF request (success or failure) keeps the controls open
 *   for 800ms so the operator can inspect the result.
 * - An unanswered AF request does not trap the controls on screen:
 *   after 2s it times out visually. A late response is ignored by the caller.
 * - AF failure is a 500ms red flash, never a reason to block AE/AF lock.
 * - Exposure dragging holds the controls open until the gesture ends.
 */
internal const val FOCUS_RESULT_HOLD_MS = 800L
internal const val FOCUS_REQUEST_TIMEOUT_MS = 2_000L
internal const val FOCUS_FAILURE_FLASH_MS = 500L

internal fun focusCollapseDelayMs(phase: FocusRingPhase): Long =
    if (phase == FocusRingPhase.FOCUSING) FOCUS_REQUEST_TIMEOUT_MS
    else FOCUS_RESULT_HOLD_MS

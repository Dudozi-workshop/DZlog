package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

internal val LocalTutorialLocation = staticCompositionLocalOf<TutorialLocation?> { null }
internal val LocalTutorialTarget = staticCompositionLocalOf<String?> { null }
internal val LocalTutorialBounds = staticCompositionLocalOf<(Rect) -> Unit> { {} }
internal val LocalTutorialDemoProgress = staticCompositionLocalOf { 0f }
internal val TutorialDemoPages = setOf("zoom", "grid", "table", "assist", "flash", "manual-focus", "move", "size", "rotation", "lock", "quick-text", "quick-number", "quick-date", "quick-phrase", "template", "saving", "preview", "ratio", "quality", "capture-confirmation", "feedback", "controls", "pinch")

internal val LocalTutorialReadOnly = staticCompositionLocalOf { false }

/** Inactive during ordinary app use. Target selection only scrolls/paints the real UI. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val selected = LocalTutorialTarget.current == id
    val reportBounds = LocalTutorialBounds.current
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
    if (selected) this.onGloballyPositioned { reportBounds(it.boundsInRoot()) }.bringIntoViewRequester(requester)
    else this
}

/** A gesture illustration only: never sends zoom gestures to the camera. */
@Composable
internal fun TutorialPinchHint(modifier: Modifier = Modifier) {
    val spread = 18f + 34f * LocalTutorialDemoProgress.current
    androidx.compose.foundation.layout.Column(
        modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(160.dp, 72.dp)) {
            val distance = spread.dp.toPx()
            listOf(-distance, distance).forEach { offset ->
                drawCircle(DDZColor.Surface.copy(alpha = 0.85f), 13.dp.toPx(),
                    androidx.compose.ui.geometry.Offset(center.x + offset, center.y))
                drawCircle(DDZColor.PrimaryDark, 13.dp.toPx(),
                    androidx.compose.ui.geometry.Offset(center.x + offset, center.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            }
        }
        androidx.compose.material3.Surface(color = DDZColor.Surface) {
            androidx.compose.material3.Text("벌리면 확대 · 모으면 축소", Modifier.padding(8.dp),
                style = com.dudoziworkshop.dzlog.ui.theme.DDZTypography.Caption)
        }
    }
}

/** A rendered table occupies only part of its full-screen Canvas. */
internal fun Modifier.tutorialRegion(id: String, region: Rect): Modifier = composed {
    val selected = LocalTutorialTarget.current == id
    val report = LocalTutorialBounds.current
    if (selected) onGloballyPositioned { report(region.translate(it.positionInRoot())) } else this
}

@Composable
internal fun GuideAwareDialog(onDismiss: () -> Unit, properties: androidx.compose.ui.window.DialogProperties,
    content: @Composable () -> Unit) {
    if (LocalTutorialReadOnly.current) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { content() }
    else androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = properties) { content() }
}

@Composable
internal fun TutorialDragHint(progress: Float) {
    androidx.compose.foundation.Canvas(Modifier.size(150.dp, 24.dp)) {
        val y = center.y
        drawLine(DDZColor.SageBorder, androidx.compose.ui.geometry.Offset(12.dp.toPx(), y),
            androidx.compose.ui.geometry.Offset(size.width - 12.dp.toPx(), y), 2.dp.toPx())
        val x = 12.dp.toPx() + (size.width - 24.dp.toPx()) * progress
        drawCircle(DDZColor.Surface, 8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
        drawCircle(DDZColor.PrimaryDark, 8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    }
}

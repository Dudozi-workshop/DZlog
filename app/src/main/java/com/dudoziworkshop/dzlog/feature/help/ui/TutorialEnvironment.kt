package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

internal val LocalTutorialLocation = staticCompositionLocalOf<TutorialLocation?> { null }
internal val LocalTutorialTarget = staticCompositionLocalOf<String?> { null }
internal val LocalTutorialBounds = staticCompositionLocalOf<(Rect) -> Unit> { {} }
internal val LocalTutorialReadOnly = staticCompositionLocalOf { false }

/** Inactive during ordinary app use. Target selection only scrolls/paints the real UI. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val selected = LocalTutorialTarget.current == id
    val reportBounds = LocalTutorialBounds.current
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
    if (selected) this.onGloballyPositioned { reportBounds(it.boundsInRoot()) }.bringIntoViewRequester(requester)
        .border(3.dp, DDZColor.PrimaryDark, RoundedCornerShape(12.dp))
    else this
}

/** A gesture illustration only: never sends zoom gestures to the camera. */
@Composable
internal fun TutorialPinchHint(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pinch-guide")
    val spread by transition.animateFloat(
        initialValue = 18f, targetValue = 52f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween<Float>(1300),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ), label = "finger-spacing",
    )
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

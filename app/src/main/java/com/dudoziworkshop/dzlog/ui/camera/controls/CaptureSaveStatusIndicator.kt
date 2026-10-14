package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay

/**
 * C-2c: a tiny, separate save indicator above the recent-capture thumbnail.
 * Displays queue state only; this component never changes reservations or saves.
 */
@Composable
internal fun CaptureSaveStatusIndicator(
    pendingCount: Int,
    hasError: Boolean,
    detailsAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSaving = pendingCount > 0
    var visible by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }

    // Skip very short saves so that the indicator does not blink on every shot.
    // Errors always appear immediately and remain visible until resolved.
    LaunchedEffect(isSaving, hasError) {
        when {
            hasError -> {
                visible = true
                detailsOpen = true
            }
            isSaving -> {
                delay(300)
                visible = true
            }
            else -> {
                visible = false
                detailsOpen = false
            }
        }
    }
    LaunchedEffect(detailsAllowed) {
        if (!detailsAllowed) detailsOpen = false
    }
    if (!visible) return

    val background = if (hasError) DDZColor.DestructiveSoft else DDZColor.SelectedSoft
    val foreground = if (hasError) DDZColor.Destructive else DDZColor.SelectionEmphasis
    val statusLabel = if (hasError) "저장 상태 확인 필요" else "${pendingCount}장 저장 중"
    val density = LocalDensity.current
    val popupPositionProvider = remember(density) {
        SaveStatusPopupPositionProvider(
            edgeInsetPx = with(density) { 12.dp.roundToPx() },
            gapPx = with(density) { 18.dp.roundToPx() },
        )
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = "$statusLabel, 자세히 보기" }
            .clickable(role = Role.Button) {
                if (detailsAllowed) detailsOpen = !detailsOpen
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 24.dp)
                .background(background, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (hasError) Icons.Filled.Warning else Icons.Filled.Save,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = if (hasError) "!" else pendingCount.toString(),
                    color = foreground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        if (detailsOpen && detailsAllowed) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { detailsOpen = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                SaveStatusDetails(
                    count = pendingCount,
                    hasError = hasError,
                    onClose = { detailsOpen = false },
                )
            }
        }
    }
}

@Composable
private fun SaveStatusDetails(
    count: Int,
    hasError: Boolean,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(216.dp),
        color = if (hasError) DDZColor.DestructiveSoft else DDZColor.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (hasError) DDZColor.Destructive.copy(alpha = 0.30f) else DDZColor.Border),
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasError) Icons.Filled.Warning else Icons.Filled.Save,
                    contentDescription = null,
                    tint = if (hasError) DDZColor.Destructive else DDZColor.SelectedDark,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (hasError) "저장 상태 확인 필요" else "사진 ${count}장 저장 중",
                    color = DDZColor.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.size(32.dp).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기",
                        tint = DDZColor.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (hasError) {
                    "일부 사진의 저장 결과를 확인해야 합니다. 원본과 촬영번호를 보호하기 위해 촬영이 제한될 수 있습니다."
                } else {
                    "촬영한 사진을 안전하게 저장하고 있어요. 완료되면 표시가 자동으로 사라집니다."
                },
                color = DDZColor.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

/** Keep the detail card above the badge, within the visible window. */
private class SaveStatusPopupPositionProvider(
    private val edgeInsetPx: Int,
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - edgeInsetPx)
            .coerceAtLeast(0)
        val minX = edgeInsetPx.coerceAtMost(maxX)
        val x = anchorBounds.left.coerceIn(minX, maxX)
        val above = anchorBounds.top - popupContentSize.height - gapPx
        val below = anchorBounds.bottom + gapPx
        val candidateY = if (above >= edgeInsetPx) above else below
        val maxY = (windowSize.height - popupContentSize.height - edgeInsetPx)
            .coerceAtLeast(0)
        return IntOffset(x, candidateY.coerceIn(0, maxY))
    }
}

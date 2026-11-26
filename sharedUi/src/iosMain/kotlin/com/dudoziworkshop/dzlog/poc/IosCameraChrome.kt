package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val CameraChromeShape = RoundedCornerShape(DDZLayout.Radius.Small)
private val CameraToolMenuShape = RoundedCornerShape(DDZLayout.Radius.Full)
private val CounterAccent = Color(0xFF2E7D50)

@Composable
internal fun IosCameraTopBar(
    displayName: String,
    infoEnabled: Boolean,
    settingsEnabled: Boolean,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onBack, enabled = backEnabled, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기",
                tint = if (backEnabled) DDZColor.SageDarkStrong else DDZColor.IconMuted)
        }
        Box(
            Modifier.weight(1f).heightIn(min = 32.dp).clip(CameraChromeShape)
                .background(DDZColor.Card.copy(alpha = 0.55f))
                .border(1.dp, DDZColor.SageBorder, CameraChromeShape)
                .clickable(enabled = infoEnabled, role = Role.Button, onClick = onOpenInfo)
                .semantics { contentDescription = "촬영 정보 열기 · $displayName" }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            CounterAwareCameraFileName(displayName)
        }
        IconButton(onClick = onOpenSettings, enabled = settingsEnabled, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "촬영 설정",
                tint = if (settingsEnabled) DDZColor.SageDarkStrong else DDZColor.IconMuted)
        }
    }
}

@Composable
private fun CounterAwareCameraFileName(displayName: String) {
    val base = displayName.removeSuffix(".jpg").removeSuffix(".jpeg")
    val match = Regex("_(\\d+)$").find(base)
    val text = buildAnnotatedString {
        if (match == null) append(base)
        else {
            append(base.removeSuffix(match.value))
            append("_")
            withStyle(SpanStyle(color = CounterAccent)) { append(match.groupValues[1]) }
        }
    }
    Text(text, style = DDZTypography.Caption, color = DDZColor.TextStrong,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun IosCameraStatusStrip(status: String, completedCount: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(status, modifier = Modifier.weight(1f), style = DDZTypography.Caption,
            color = DDZColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("이번 촬영: ${completedCount}장", style = DDZTypography.Caption,
            color = DDZColor.SageDarkStrong)
    }
}

@Composable
internal fun IosCameraProjectBar(
    projectName: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ).semantics { contentDescription = "카메라 프로젝트 변경" }
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp),
            tint = if (enabled) DDZColor.SageDarkStrong else DDZColor.IconMuted)
        Text("촬영 프로젝트", style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        Text(projectName?.takeIf(String::isNotBlank) ?: "일반 촬영",
            modifier = Modifier.weight(1f), style = DDZTypography.Caption,
            color = DDZColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("변경", style = DDZTypography.Caption,
            color = if (enabled) DDZColor.PrimaryDark else DDZColor.TextMuted)
    }
}

@Composable
internal fun IosCameraZoomChip(
    zoom: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        color = DDZColor.OverlayScrim.copy(alpha = 0.72f),
        shape = CircleShape,
    ) {
        Text("${formatIosZoom(zoom)}×", modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = DDZTypography.SegmentSmall, color = DDZColor.OverlayText)
    }
}

@Composable
internal fun IosCameraToolPanel(
    ready: Boolean,
    flashAvailable: Boolean,
    flashLabel: String,
    zoom: Float,
    maximumZoom: Float,
    showGrid: Boolean,
    showTable: Boolean,
    assistShutterEnabled: Boolean,
    onAutoFocus: () -> Unit,
    onCycleFlash: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onZoomFinished: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleTable: () -> Unit,
    onToggleAssistShutter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(0.90f),
        color = DDZColor.Surface.copy(alpha = 0.94f),
        shape = CameraToolMenuShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, DDZColor.SageBorder),
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CameraToolAction("초점", "자동초점", Icons.Default.CenterFocusStrong, ready, onAutoFocus)
                CameraToolAction("플래시", "플래시 $flashLabel", when (flashLabel) {
                    "자동" -> Icons.Default.FlashAuto
                    "켜짐" -> Icons.Default.FlashOn
                    else -> Icons.Default.FlashOff
                }, ready && flashAvailable, onCycleFlash, selected = flashLabel != "꺼짐")
                CameraToolAction("격자", if (showGrid) "격자 숨기기" else "격자 표시",
                    Icons.Default.GridOn, true, onToggleGrid, selected = showGrid)
                CameraToolAction("표", if (showTable) "표 숨기기" else "표 표시",
                    Icons.Default.TableChart, true, onToggleTable, selected = showTable)
                CameraToolAction("보조", if (assistShutterEnabled) "보조 셔터 끄기" else "보조 셔터 켜기",
                    Icons.Default.CameraAlt, true, onToggleAssistShutter,
                    selected = assistShutterEnabled)
            }
            Slider(
                value = zoom.coerceIn(1f, maxOf(1.01f, maximumZoom)),
                valueRange = 1f..maxOf(1.01f, maximumZoom),
                enabled = ready && maximumZoom > 1f,
                onValueChange = onZoomChange,
                onValueChangeFinished = onZoomFinished,
            )
        }
    }
}

@Composable
private fun RowScope.CameraToolAction(
    label: String,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val background = if (selected) DDZColor.SageLight.copy(alpha = 0.74f) else Color.Transparent
    val tint = if (selected) DDZColor.SageDarkStrong else DDZColor.SageDark
    Box(
        Modifier.weight(1f).heightIn(min = 48.dp).clip(CameraToolMenuShape)
            .background(background).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription; role = Role.Button }
            .padding(horizontal = 2.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = if (enabled) tint else DDZColor.IconMuted.copy(alpha = 0.38f))
            Text(label, style = DDZTypography.Caption,
                color = if (enabled) tint else DDZColor.TextMuted.copy(alpha = 0.38f), maxLines = 1)
        }
    }
}

@Composable
internal fun IosCameraBottomBar(
    captureEnabled: Boolean,
    controlsEnabled: Boolean,
    toolsSelected: Boolean,
    quickValuesEnabled: Boolean,
    undoEnabled: Boolean,
    pendingSaveCount: Int,
    saveNeedsReview: Boolean,
    saveQueueFull: Boolean,
    saveQueueCapacity: Int,
    saveDetailsAllowed: Boolean,
    recentThumbnail: ImageBitmap?,
    galleryLabel: String,
    onOpenGallery: () -> Unit,
    onToggleTools: () -> Unit,
    onCapture: () -> Unit,
    onOpenQuickValues: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(18f), contentAlignment = Alignment.Center) {
            IosCameraGalleryButton(recentThumbnail, galleryLabel, controlsEnabled, onOpenGallery)
            IosCameraSaveStatusIndicator(
                pendingCount = pendingSaveCount,
                hasError = saveNeedsReview,
                atCapacity = saveQueueFull,
                capacity = saveQueueCapacity,
                detailsAllowed = saveDetailsAllowed,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp),
            )
        }
        Box(Modifier.weight(18f), contentAlignment = Alignment.Center) {
            IosCameraChromeButton("촬영 도구", Icons.Default.Tune, controlsEnabled, onToggleTools,
                selected = toolsSelected)
        }
        Box(Modifier.weight(28f), contentAlignment = Alignment.Center) {
            Button(
                onClick = onCapture,
                enabled = captureEnabled,
                modifier = Modifier.size(70.dp).semantics { contentDescription = "촬영" },
                shape = CircleShape,
                contentPadding = PaddingValues(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DDZColor.SagePrimary,
                    disabledContainerColor = DDZColor.IconMuted,
                ),
            ) {
                Box(Modifier.fillMaxSize().background(
                    if (captureEnabled) Color.White else DDZColor.Border, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("●", modifier = Modifier.clearAndSetSemantics {},
                        color = if (captureEnabled) DDZColor.SagePrimary else DDZColor.TextMuted,
                        style = DDZTypography.CardTitle)
                }
            }
        }
        Box(Modifier.weight(18f), contentAlignment = Alignment.Center) {
            IosCameraChromeButton("빠른 값 변경", Icons.Default.Edit, quickValuesEnabled, onOpenQuickValues)
        }
        Box(Modifier.weight(18f), contentAlignment = Alignment.Center) {
            IosCameraChromeButton("직전 촬영 삭제", Icons.AutoMirrored.Filled.Undo,
                undoEnabled, onUndo)
        }
    }
}

/** iOS counterpart of Android C-2c; queue display only, with no save side effects. */
@Composable
private fun IosCameraSaveStatusIndicator(
    pendingCount: Int,
    hasError: Boolean,
    atCapacity: Boolean,
    capacity: Int,
    detailsAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    if (pendingCount == 0 && !hasError) return
    var detailsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(detailsAllowed) {
        if (!detailsAllowed) detailsOpen = false
    }
    val background = if (hasError) DDZColor.DestructiveSoft else DDZColor.SelectedSoft
    val foreground = if (hasError) DDZColor.Destructive else DDZColor.SelectionEmphasis
    val label = when {
        hasError -> "저장 상태 확인 필요"
        atCapacity -> "${pendingCount}장 저장 중, 대기열 가득 참"
        else -> "${pendingCount}장 저장 중"
    }
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.height(24.dp).clip(CircleShape)
                .clickable(enabled = detailsAllowed, role = Role.Button) {
                    detailsOpen = !detailsOpen
                }.semantics { contentDescription = "$label, 자세히 보기" },
            color = background,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(if (hasError) Icons.Default.Warning else Icons.Default.Save,
                    contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
                Text(if (hasError) "!" else if (atCapacity) "$pendingCount/$capacity" else pendingCount.toString(),
                    color = foreground, fontSize = 11.sp, maxLines = 1)
            }
        }
        val density = LocalDensity.current
        val popupOffset = remember(density) {
            IntOffset(with(density) { 12.dp.roundToPx() }, with(density) { (-112).dp.roundToPx() })
        }
        if (detailsOpen && detailsAllowed) Popup(
            alignment = Alignment.BottomStart,
            offset = popupOffset,
            onDismissRequest = { detailsOpen = false },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier.width(220.dp),
                color = if (hasError) DDZColor.DestructiveSoft else DDZColor.Surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp,
                    if (hasError) DDZColor.Destructive.copy(alpha = 0.30f) else DDZColor.Border),
                shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (hasError) Icons.Default.Warning else Icons.Default.Save,
                            contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(label, modifier = Modifier.weight(1f), style = DDZTypography.Caption,
                            color = DDZColor.TextPrimary)
                        IconButton(onClick = { detailsOpen = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "닫기",
                                tint = DDZColor.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(when {
                        hasError -> "원본과 촬영번호를 보호하기 위해 저장 결과 확인 전까지 촬영이 제한됩니다."
                        atCapacity -> "${capacity}장 모두 저장 중입니다. 한 장의 저장이 완료되면 셔터가 다시 활성화됩니다."
                        else -> "촬영한 사진을 순서대로 안전하게 저장하고 있습니다."
                    }, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun IosCameraGalleryButton(
    thumbnail: ImageBitmap?,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Box(
            Modifier.size(40.dp).clip(CameraChromeShape)
                .border(1.dp, DDZColor.SageBorder, CameraChromeShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) Image(
                bitmap = thumbnail,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) else Icon(Icons.Default.Image, contentDescription = label, modifier = Modifier.size(22.dp),
                tint = if (enabled) DDZColor.SageDarkStrong else DDZColor.IconMuted.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun IosCameraChromeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Box(
            Modifier.size(40.dp).clip(CameraChromeShape)
                .border(1.dp, if (selected) DDZColor.SageDarkStrong else DDZColor.SageBorder, CameraChromeShape)
                .background(if (selected) DDZColor.SageLight else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp),
                tint = if (enabled) DDZColor.SageDarkStrong else DDZColor.IconMuted.copy(alpha = 0.35f))
        }
    }
}

private fun formatIosZoom(value: Float): String {
    val tenths = (value * 10f).toInt()
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}

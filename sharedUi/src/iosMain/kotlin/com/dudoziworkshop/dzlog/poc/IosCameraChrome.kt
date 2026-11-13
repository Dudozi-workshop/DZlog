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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val CameraChromeShape = RoundedCornerShape(DDZLayout.Radius.Small)
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
        Text("카메라", style = DDZTypography.SettingLabel, color = DDZColor.TextStrong)
        Box(
            Modifier.weight(1f).heightIn(min = 32.dp).clip(CameraChromeShape)
                .background(DDZColor.Card.copy(alpha = 0.55f))
                .border(1.dp, DDZColor.SageBorder, CameraChromeShape)
                .clickable(enabled = infoEnabled, onClick = onOpenInfo)
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
    onAutoFocus: () -> Unit,
    onCycleFlash: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        color = DDZColor.OverlayScrim.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CameraToolAction("자동초점", Icons.Default.CenterFocusStrong, ready, onAutoFocus)
                CameraToolAction("플래시 $flashLabel", Icons.Default.WbSunny,
                    ready && flashAvailable, onCycleFlash)
                CameraToolStatus("줌 ${formatIosZoom(zoom)}배", Icons.Default.Tune,
                    ready && maximumZoom > 1f)
            }
            Slider(
                value = zoom.coerceIn(1f, maxOf(1.01f, maximumZoom)),
                valueRange = 1f..maxOf(1.01f, maximumZoom),
                enabled = ready && maximumZoom > 1f,
                onValueChange = onZoomChange,
            )
        }
    }
}

@Composable
private fun RowScope.CameraToolStatus(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
) {
    Column(Modifier.weight(1f).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val tint = if (enabled) DDZColor.OverlayText else DDZColor.OverlayText.copy(alpha = 0.38f)
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Text(label, style = DDZTypography.Caption, color = tint, maxLines = 1)
    }
}

@Composable
private fun RowScope.CameraToolAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.textButtonColors(
            contentColor = DDZColor.OverlayText,
            disabledContentColor = DDZColor.OverlayText.copy(alpha = 0.38f),
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = DDZTypography.Caption, maxLines = 1)
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
    recentThumbnail: ImageBitmap?,
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
            IosCameraGalleryButton(recentThumbnail, controlsEnabled, onOpenGallery)
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
            IosCameraChromeButton("빠른 입력", Icons.Default.Edit, quickValuesEnabled, onOpenQuickValues)
        }
        Box(Modifier.weight(18f), contentAlignment = Alignment.Center) {
            IosCameraChromeButton("직전 촬영 삭제", Icons.AutoMirrored.Filled.Undo,
                undoEnabled, onUndo)
        }
    }
}

@Composable
private fun IosCameraGalleryButton(
    thumbnail: ImageBitmap?,
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
                contentDescription = "앨범 보기",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) else Icon(Icons.Default.Image, contentDescription = "앨범 보기", modifier = Modifier.size(22.dp),
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

package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.data.template.EditorCaptureAnchor
import com.dudoziworkshop.dzlog.data.template.EditorCapturePlacement
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControl
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControlOption
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControlStyles
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

private val IosCameraSettingsShape = RoundedCornerShape(DDZLayout.Radius.Medium)
private val IosCameraSettingsSegments = DDZSegmentedControlStyles.CameraPanel.copy(
    widthFraction = 1f,
    fixedHeight = 40.dp,
    minItemHeight = 40.dp,
    outerHorizontalPadding = 0.dp,
    innerHorizontalPadding = 1.dp,
    innerVerticalPadding = 0.dp,
    itemHorizontalPadding = 1.dp,
    itemSpacing = 1.dp,
    selectedContainerColor = DDZColor.SageLight,
    textStyle = DDZTypography.Caption.copy(fontSize = 11.sp),
)

/** Android camera-settings hierarchy with iOS-only system and placement details. */
@Composable
internal fun IosCaptureSettingsScreen(
    document: EditorDocument,
    continuousPreviewMode: Int,
    captureHapticEnabled: Boolean,
    captureAspectMode: Int,
    onBack: () -> Unit,
    onApply: (EditorDocument, Int, Boolean, Int) -> Unit,
) {
    var mode by remember(document) { mutableStateOf(document.saveMode) }
    var placement by remember(document) { mutableStateOf(document.capturePlacement ?:
        EditorCapturePlacement(EditorCaptureAnchor.BOTTOM_RIGHT, 50, 50, 60, 20)) }
    var number by remember(document) { mutableStateOf((document.nextCounter ?: 1).toString()) }
    var previewMode by remember(continuousPreviewMode) { mutableIntStateOf(continuousPreviewMode) }
    var hapticEnabled by remember(captureHapticEnabled) { mutableStateOf(captureHapticEnabled) }
    var captureAspect by remember(captureAspectMode) { mutableStateOf(CaptureAspect.from(captureAspectMode)) }
    val next = number.toIntOrNull()?.takeIf { it > 0 && it < Int.MAX_VALUE }
    val layoutError = runCatching { iosCaptureCellRects(document.template.templateState) }.exceptionOrNull()?.message
    val saveEnabled = next != null && (mode == SaveMode.ORIGINAL_ONLY || layoutError == null)

    Box(
        modifier = Modifier.fillMaxSize().background(DDZColor.PrimaryDark.copy(alpha = 0.20f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 380.dp).heightIn(max = 640.dp),
            shape = IosCameraSettingsShape,
            color = DDZColor.Card,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack, modifier = Modifier.width(56.dp)) { Text("취소") }
                    Text(
                        text = "촬영 설정",
                        modifier = Modifier.weight(1f),
                        style = DDZTypography.OverlayTitleCompact,
                        color = DDZColor.Primary,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(
                        enabled = saveEnabled,
                        modifier = Modifier.width(56.dp),
                        onClick = {
                            onApply(
                                document.copy(
                                    saveMode = mode,
                                    nextCounter = next,
                                    usesAutoNext = if (next != document.nextCounter) false else document.usesAutoNext,
                                    capturePlacement = if (mode == SaveMode.ORIGINAL_ONLY) document.capturePlacement else placement,
                                ),
                                previewMode,
                                hapticEnabled,
                                captureAspect.v,
                            )
                        },
                    ) { Text("저장") }
                }

                HorizontalDivider(color = DDZColor.Border)
                Text(
                    text = document.template.name,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    maxLines = 1,
                )

                IosCaptureSettingsOptions(
                    captureAspect, mode, previewMode, hapticEnabled,
                    onAspectChange = { captureAspect = it },
                    onModeChange = { mode = it },
                    onPreviewChange = { previewMode = it },
                    onHapticChange = { hapticEnabled = it },
                )

                HorizontalDivider(color = DDZColor.Border)
                Text(
                    text = "표·번호 설정",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    style = DDZTypography.SettingLabel,
                    color = DDZColor.Primary,
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { if (it.all(Char::isDigit)) number = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("다음 촬영 번호") },
                    singleLine = true,
                    isError = next == null,
                )

                if (mode != SaveMode.ORIGINAL_ONLY) {
                    layoutError?.let {
                        Text(it, style = DDZTypography.Caption, color = DDZColor.Destructive)
                    }
                    Text("표 위치", style = DDZTypography.SettingLabel, color = DDZColor.Primary)
                    IosCompactSegments(listOf(
                        DDZSegmentedControlOption("왼쪽 위", placement.anchor == EditorCaptureAnchor.TOP_LEFT) {
                            placement = placement.copy(anchor = EditorCaptureAnchor.TOP_LEFT, rotationCwDeg = 0)
                        },
                        DDZSegmentedControlOption("오른쪽 위", placement.anchor == EditorCaptureAnchor.TOP_RIGHT) {
                            placement = placement.copy(anchor = EditorCaptureAnchor.TOP_RIGHT, rotationCwDeg = 0)
                        },
                    ))
                    IosCompactSegments(listOf(
                        DDZSegmentedControlOption("왼쪽 아래", placement.anchor == EditorCaptureAnchor.BOTTOM_LEFT) {
                            placement = placement.copy(anchor = EditorCaptureAnchor.BOTTOM_LEFT, rotationCwDeg = 0)
                        },
                        DDZSegmentedControlOption("오른쪽 아래", placement.anchor == EditorCaptureAnchor.BOTTOM_RIGHT) {
                            placement = placement.copy(anchor = EditorCaptureAnchor.BOTTOM_RIGHT, rotationCwDeg = 0)
                        },
                    ))
                    IosFeedbackChip(
                        label = "사용자 지정",
                        selected = placement.anchor == EditorCaptureAnchor.CUSTOM,
                        enabled = true,
                        onClick = { placement = placement.copy(anchor = EditorCaptureAnchor.CUSTOM, rotationCwDeg = 0) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CaptureRatio("표 너비", placement.widthRatio, 10f..100f) {
                        placement = placement.copy(widthRatio = it)
                    }
                    CaptureRatio("표 높이", placement.heightRatio, 10f..100f) {
                        placement = placement.copy(heightRatio = it)
                    }
                    Text("표 너비와 높이는 사진 너비를 기준으로 적용됩니다.",
                        style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    if (placement.anchor == EditorCaptureAnchor.CUSTOM) {
                        CaptureRatio("가로 위치", placement.offsetXPercent, 0f..100f) {
                            placement = placement.copy(offsetXPercent = it)
                        }
                        CaptureRatio("세로 위치", placement.offsetYPercent, 0f..100f) {
                            placement = placement.copy(offsetYPercent = it)
                        }
                        IosFeedbackChip(
                            label = "90도 회전",
                            selected = placement.rotationCwDeg == 90,
                            enabled = true,
                            onClick = { placement = placement.copy(rotationCwDeg = if (placement.rotationCwDeg == 90) 0 else 90) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Keeps the live camera mounted while the common options update the session. */
@Composable
internal fun IosCaptureSettingsOverlay(
    captureAspect: CaptureAspect,
    mode: SaveMode,
    previewMode: Int,
    hapticEnabled: Boolean,
    error: String?,
    onAspectChange: (CaptureAspect) -> Unit,
    onModeChange: (SaveMode) -> Unit,
    onPreviewChange: (Int) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onOpenDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.matchParentSize().background(DDZColor.PrimaryDark.copy(alpha = 0.20f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            )
            BoxWithConstraints(
                Modifier.align(Alignment.TopEnd).safeDrawingPadding()
            ) {
                val panelWidth = (maxWidth * 0.72f).coerceIn(minOf(240.dp, maxWidth), 320.dp)
                Surface(
                    modifier = Modifier.width(panelWidth).heightIn(max = 440.dp),
                    shape = IosCameraSettingsShape,
                    color = DDZColor.Card,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                            Text("촬영 설정", style = DDZTypography.OverlayTitleCompact, color = DDZColor.Primary)
                            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                                Text("닫기")
                            }
                        }
                        HorizontalDivider(color = DDZColor.Border)
                        IosCaptureSettingsOptions(
                            captureAspect, mode, previewMode, hapticEnabled,
                            onAspectChange, onModeChange, onPreviewChange, onHapticChange,
                        )
                        error?.let { Text(it, style = DDZTypography.Caption, color = DDZColor.Destructive) }
                        TextButton(onClick = onOpenDetails, modifier = Modifier.align(Alignment.End)) {
                            Text("표·번호 설정")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosCaptureSettingsOptions(
    captureAspect: CaptureAspect,
    mode: SaveMode,
    previewMode: Int,
    hapticEnabled: Boolean,
    onAspectChange: (CaptureAspect) -> Unit,
    onModeChange: (SaveMode) -> Unit,
    onPreviewChange: (Int) -> Unit,
    onHapticChange: (Boolean) -> Unit,
) {
    IosCompactSettingsRow("비율") {
        IosRowSegments(listOf(CaptureAspect.R1_1, CaptureAspect.R3_4, CaptureAspect.R9_16).map { value ->
            DDZSegmentedControlOption(value.label, captureAspect == value) { onAspectChange(value) }
        })
    }
    IosCompactSettingsRow("저장") {
        IosRowSegments(listOf(
            DDZSegmentedControlOption("원본", mode == SaveMode.ORIGINAL_ONLY) { onModeChange(SaveMode.ORIGINAL_ONLY) },
            DDZSegmentedControlOption("표 합성", mode == SaveMode.WATERMARK_ONLY) { onModeChange(SaveMode.WATERMARK_ONLY) },
            DDZSegmentedControlOption("둘 다", mode == SaveMode.BOTH) { onModeChange(SaveMode.BOTH) },
        ))
    }
    IosCompactSettingsRow("촬영 확인") {
        IosRowSegments(listOf(
            DDZSegmentedControlOption("안 함", previewMode == 0) { onPreviewChange(0) },
            DDZSegmentedControlOption("잠깐", previewMode == 1) { onPreviewChange(1) },
            DDZSegmentedControlOption("유지", previewMode == 2) { onPreviewChange(2) },
        ))
    }
    IosCompactSettingsRow("피드백") {
        IosFeedbackChip("소리·시스템", selected = true, enabled = false, onClick = {},
            modifier = Modifier.weight(1f))
        IosFeedbackChip("진동", selected = hapticEnabled, enabled = true,
            onClick = { onHapticChange(!hapticEnabled) }, modifier = Modifier.weight(1f))
    }
    Text(
        text = "촬영음은 iPhone 시스템 설정과 지역 정책을 따릅니다.",
        modifier = Modifier.fillMaxWidth().padding(start = 61.dp, end = 4.dp, bottom = 4.dp),
        style = DDZTypography.Caption,
        color = DDZColor.TextMuted,
    )
}

@Composable
private fun IosCompactSettingsRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.width(55.dp).height(44.dp), contentAlignment = Alignment.Center) {
            Text(label, style = DDZTypography.Caption.copy(fontSize = 11.sp), color = DDZColor.Primary,
                textAlign = TextAlign.Center, maxLines = 1)
        }
        content()
    }
}

@Composable
private fun RowScope.IosRowSegments(options: List<DDZSegmentedControlOption>) {
    DDZSegmentedControl(options = options, modifier = Modifier.weight(1f), style = IosCameraSettingsSegments)
}

@Composable
private fun IosCompactSegments(options: List<DDZSegmentedControlOption>) {
    DDZSegmentedControl(options = options, modifier = Modifier.fillMaxWidth(), style = IosCameraSettingsSegments)
}

@Composable
private fun IosFeedbackChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(DDZLayout.Radius.Small)
    Box(
        modifier = modifier.height(40.dp)
            .background(if (selected) DDZColor.SageLight else DDZColor.Surface, shape)
            .border(BorderStroke(1.dp, if (selected) DDZColor.SageBorder else DDZColor.Border), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = DDZTypography.Caption.copy(fontSize = 11.sp),
            color = if (selected) DDZColor.SageDarkStrong else DDZColor.TextMuted, maxLines = 1)
    }
}

@Composable
private fun CaptureRatio(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = DDZTypography.Caption, color = DDZColor.TextPrimary)
        Text("$value%", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    }
    Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = range)
}

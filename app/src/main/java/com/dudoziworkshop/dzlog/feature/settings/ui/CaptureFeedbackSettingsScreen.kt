package com.dudoziworkshop.dzlog.feature.settings.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.BuildConfig
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureSoundDiagnostics
import com.dudoziworkshop.dzlog.feature.capture.policy.ShutterRequirementStatus
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import com.dudoziworkshop.dzlog.data.datastore.CaptureVibrationStrength
import com.dudoziworkshop.dzlog.ui.camera.CaptureFeedback
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Shared destination for root Settings and the existing camera settings overlay.
 * Reads the same AppSettingsStore state as the shutter, without touching media data.
 */
@Composable
fun CaptureFeedbackSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(initial = AppSettings.Default)
    val player = remember(context) { CaptureFeedback(context) }
    val shutterProbe = remember { CaptureFeedback.shutterRequirementProbe() }
    val lastExposure by CaptureSoundDiagnostics.lastExposure.collectAsState()
    DisposableEffect(player) { onDispose { player.release() } }
    BackHandler(onBack = onBack)
    var draftVolume by remember { mutableFloatStateOf(settings.captureSoundVolume.toFloat()) }
    LaunchedEffect(settings.captureSoundVolume) { draftVolume = settings.captureSoundVolume.toFloat() }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = "촬영 효과음 및 진동", onBack = onBack) },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("촬영 효과음", style = DDZTypography.Body, color = DDZColor.PrimaryDark)
            Surface(
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Column {
                    CaptureSoundChoice.entries.forEachIndexed { index, choice ->
                        if (index > 0) HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = DDZColor.Border.copy(alpha = 0.55f),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = choice.available) {
                                    scope.launch { AppSettingsStore.setCaptureSoundChoice(context, choice) }
                                    if (!player.preview(choice, draftVolume.roundToInt())) {
                                        Toast.makeText(context, "미리 듣기를 재생하지 못했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (settings.captureSoundChoice == choice)
                                    Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (settings.captureSoundChoice == choice)
                                    DDZColor.Selected else DDZColor.IconMuted,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = choice.label,
                                    color = if (choice.available) DDZColor.TextPrimary else DDZColor.TextDisabled,
                                    style = DDZTypography.Body,
                                )
                                if (!choice.available) Text(
                                    "승인된 원본 파일 복원 후 사용 가능",
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextSecondary,
                                )
                            }
                            IconButton(
                                enabled = choice.available,
                                onClick = {
                                    if (!player.preview(choice, draftVolume.roundToInt())) {
                                        Toast.makeText(context, "효과음 준비 중입니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = choice.label + " 미리 듣기",
                                    tint = if (choice.available) DDZColor.PrimaryDark else DDZColor.TextDisabled,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "촬영 효과음 음량  ${draftVolume.roundToInt()}%",
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
            )
            Slider(
                value = draftVolume,
                onValueChange = { draftVolume = it },
                onValueChangeFinished = {
                    scope.launch {
                        AppSettingsStore.setCaptureSoundVolume(context, draftVolume.roundToInt())
                    }
                },
                valueRange = 0f..100f,
                enabled = true,
                colors = SliderDefaults.colors(
                    thumbColor = DDZColor.Selected,
                    activeTrackColor = DDZColor.Selected,
                ),
            )
            Text(
                "기본 셔터음을 포함한 5종 모두 같은 음량 설정을 사용합니다. 미리 듣기는 기기 미디어 음량을 따릅니다.",
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
            )
            Text(
                when (shutterProbe.status) {
                    ShutterRequirementStatus.REQUIRED ->
                        "Android가 이 기기에서 촬영음을 요구한다고 보고했습니다. 실제 촬영에서는 기본 시스템음을 재생합니다."
                    ShutterRequirementStatus.QUERY_FAILED ->
                        "촬영음 요구 여부를 조회하지 못해 기본 시스템음으로 처리합니다. 아래 QA 진단을 확인하세요."
                    ShutterRequirementStatus.NOT_REQUIRED ->
                        "선택한 DZlog 효과음만 재생하며, 소리 OFF 또는 음량 0%에서는 앱 효과음이 없습니다."
                },
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
            )
            if (BuildConfig.DEBUG) {
                Text("QA · 촬영음 진단", style = DDZTypography.Body, color = DDZColor.PrimaryDark)
                Text(
                    "현재 기기 판정: ${shutterProbe.status}" +
                        (shutterProbe.errorType?.let { " (조회 오류: $it)" } ?: ""),
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
                Text(
                    lastExposure?.let { record ->
                        val playback = when (record.playbackStarted) {
                            true -> "재생 호출 성공(실제 청감 미확인)"
                            false -> "재생 요청 실패"
                            null -> "재생 요청 없음"
                        }
                        "마지막 촬영: 판정=${record.requirement.status}, " +
                            "효과음=${if (record.enabled) "ON" else "OFF"}, " +
                            "선택=${record.selected.label}, 음량=${record.volume}%, " +
                            "실제 경로=${record.route}, $playback" +
                            (record.requirement.errorType?.let { ", 오류=$it" } ?: "")
                    } ?: "마지막 촬영 기록 없음. 카메라로 촬영한 뒤 다시 확인하세요.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
            Text("촬영 진동", style = DDZTypography.Body, color = DDZColor.PrimaryDark)
            val selectedStrength = if (settings.captureHapticEnabled)
                settings.captureVibrationStrength else CaptureVibrationStrength.OFF
            Surface(
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CaptureVibrationStrength.entries.forEach { strength ->
                        val selected = selectedStrength == strength
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                // Demonstrate immediately, independently from media capture.
                                // A disabled global app-haptic setting does not suppress preview.
                                if (!player.previewVibration(strength)) {
                                    Toast.makeText(context, "기기 진동을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                                scope.launch {
                                    AppSettingsStore.setCaptureVibrationStrength(context, strength)
                                }
                            },
                            color = if (selected) DDZColor.SelectedSoft else DDZColor.Background,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) DDZColor.Selected else DDZColor.Border),
                        ) {
                            Text(
                                strength.label,
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = DDZTypography.Caption,
                                color = if (selected) DDZColor.SelectedDark else DDZColor.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
            if (!settings.hapticEnabled) {
                Text(
                    "앱 전체 진동이 꺼져 있어 촬영 진동이 실행되지 않습니다.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
            Text(
                "카메라 화면의 소리·진동 버튼으로 빠르게 켜거나 끌 수 있으며, 다시 켜면 선택한 설정이 유지됩니다.",
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
            )
        }
    }
}

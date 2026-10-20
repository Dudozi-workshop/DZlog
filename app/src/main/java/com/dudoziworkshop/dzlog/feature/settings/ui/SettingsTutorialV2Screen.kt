package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Separate from gallery tutorial. This preview never reads or mutates AppSettingsStore. */
internal data class SettingsGuideItem(
    val section: String,
    val title: String,
    val description: String,
)

internal val SETTINGS_GUIDE_ITEMS = listOf(
    SettingsGuideItem("앱 환경", "권한 관리", "카메라와 사진 접근 권한을 확인하고 관리할 수 있어요. 사진 저장에 필요한 권한이나 폴더 연결 상태도 여기에서 확인해요."),
    SettingsGuideItem("앱 환경", "사진 품질", "사진 저장 품질을 속도 우선, 균형, 화질 우선 중에서 선택할 수 있어요. 화질이 높을수록 저장 시간이 길어질 수 있어요."),
    SettingsGuideItem("앱 환경", "플로팅 보조 셔터", "촬영 화면에 표시되는 보조 셔터 버튼을 켜거나 끌 수 있어요. 원하는 위치에서 편리하게 촬영할 때 사용하는 기능이에요."),
    SettingsGuideItem("촬영 설정", "촬영 효과음 및 진동", "촬영할 때 사용할 효과음과 음량, 진동 강도를 설정할 수 있어요. 효과음은 저장하기 전에 미리 들어볼 수도 있어요."),
    SettingsGuideItem("촬영 설정", "저장경로 변경 시 삭제 경고", "촬영 중 저장 위치가 변경된 경우, 실행 취소로 다른 폴더의 사진을 삭제하기 전에 확인 메시지를 표시하는 기능이에요."),
    SettingsGuideItem("데이터 관리", "작업 환경 내보내기", "현재 사용 중인 표 양식과 저장 규칙을 백업 파일로 저장할 수 있어요. 촬영한 사진 자체를 백업하는 기능은 아니에요."),
    SettingsGuideItem("데이터 관리", "작업 환경 가져오기", "이전에 백업한 표 양식과 저장 규칙을 다시 가져올 수 있어요. 다른 기기에서 작업 환경을 준비할 때도 활용할 수 있어요."),
    SettingsGuideItem("정보 및 지원", "사용법 및 도움말", "처음 사용법을 다시 보거나 카메라, 표 편집, 갤러리 등 기능별 튜토리얼을 선택해서 살펴볼 수 있어요."),
    SettingsGuideItem("정보 및 지원", "도움 주신 분들", "DZlog 제작에 도움을 주신 분들과 화면 아래의 앱 버전 정보를 확인할 수 있어요."),
)

/**
 * Read-only settings tour. A separate settings-like preview prevents accidental writes,
 * permission requests, file picking, purchases, and destructive Undo.
 */
@Composable
internal fun SettingsTutorialV2Screen(onBack: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = SETTINGS_GUIDE_ITEMS[index]
    var targetBounds by remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    BackHandler { onBack() }
    Box(modifier = Modifier.fillMaxSize()) {
        // Render the actual settings screen. The transparent input shield consumes all
        // touches so settings, permissions, purchases and backup pickers cannot run.
        SettingsRootScreen(
            onBack = {},
            onOpenCredits = {},
            onOpenGuide = {},
            onOpenSettingsTour = {},
            onOpenExport = {},
            onOpenImport = {},
            tutorialTarget = current.title,
            onTutorialTargetBounds = { targetBounds = it },
        )
        Box(
            modifier = Modifier.fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.20f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        )
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "전체 설정 · ${index + 1}/${SETTINGS_GUIDE_ITEMS.size}",
                        style = DDZTypography.SettingLabel,
                        color = DDZColor.PrimaryDark,
                    )
                    androidx.compose.material3.TextButton(onClick = onBack) { Text("닫기") }
                }
            }
        }
        val popupAbove = targetBounds?.center?.y?.let { it > with(density) { 390.dp.toPx() } } ?: (index >= 4)
        Surface(
            modifier = Modifier.align(if (popupAbove) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 76.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.Surface,
            border = BorderStroke(2.dp, DDZColor.PrimaryDark),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(current.section, style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                Text(current.title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                Text(current.description, style = DDZTypography.Body, color = DDZColor.TextSecondary)
                Text(
                    "실제 설정 화면을 보여주는 읽기 전용 안내입니다.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = index > 0,
                        onClick = { index-- },
                    ) { Text("이전") }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (index == SETTINGS_GUIDE_ITEMS.lastIndex) onBack() else index++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                    ) { Text(if (index == SETTINGS_GUIDE_ITEMS.lastIndex) "마치기" else "다음") }
                }
            }
        }
    }
}

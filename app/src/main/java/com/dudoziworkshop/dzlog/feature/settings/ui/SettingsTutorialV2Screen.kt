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
    SettingsGuideItem("앱 환경", "권한 관리", "카메라와 사진 접근 권한 및 폴더 연결 상태를 확인해요."),
    SettingsGuideItem("앱 환경", "사진 품질", "속도 우선·균형·화질 우선 중 저장 방식에 맞게 선택할 수 있어요."),
    SettingsGuideItem("앱 환경", "플로팅 보조 셔터", "촬영 화면의 보조 셔터 버튼 표시 여부를 정해요."),
    SettingsGuideItem("촬영 설정", "촬영 효과음 및 진동", "셔터 효과음 미리 듣기, 음량과 진동 강도를 설정할 수 있어요."),
    SettingsGuideItem("촬영 설정", "저장경로 변경 시 삭제 경고", "다른 폴더에 저장된 사진을 실행 취소할 때 확인을 표시해요."),
    SettingsGuideItem("데이터 관리", "작업 환경 내보내기", "표 템플릿과 저장 규칙을 백업하는 기능이에요. 사진 백업과는 달라요."),
    SettingsGuideItem("데이터 관리", "작업 환경 가져오기", "백업한 작업 환경을 가져올 수 있어요. 여기서는 실제 가져오지 않아요."),
    SettingsGuideItem("정보 및 지원", "사용법 및 도움말", "기능별 안내와 처음 사용법을 다시 확인할 수 있어요."),
    SettingsGuideItem("정보 및 지원", "도움 주신 분들", "DZlog 제작에 도움을 주신 분들을 확인할 수 있어요."),
)

/**
 * Read-only settings tour. A separate settings-like preview prevents accidental writes,
 * permission requests, file picking, purchases, and destructive Undo.
 */
@Composable
internal fun SettingsTutorialV2Screen(onBack: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = SETTINGS_GUIDE_ITEMS[index]
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
        Surface(
            modifier = Modifier.align(if (index < 4) Alignment.BottomCenter else Alignment.Center)
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

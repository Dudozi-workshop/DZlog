package com.example.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [CameraTopBar]
 * - 목적: 촬영 화면 상단의 네비게이션 UI(뒤로/촬영 설정)를 표시함
 * - 포함: 버튼 UI 렌더 + 클릭 콜백 연결
 * - 제외: CameraX/저장/상태 보유 로직 금지(표시 전용)
 */
@Composable
internal fun CameraTopBar(
    onExitToHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(
                top = DDZSpacing.screenPadding + DDZSpacing.sectionGap + DDZSpacing.itemGap,
                start = DDZSpacing.screenPadding,
                end = DDZSpacing.screenPadding
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                .clickable { onExitToHome() }
                .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
        ) {
            Text("뒤로", style = DDZTypography.ButtonText, color = DDZColor.Surface)
        }

        Box(
            modifier = Modifier
                .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                .clickable { onOpenSettings() }
                .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
        ) {
            Text("촬영 설정", style = DDZTypography.ButtonText, color = DDZColor.Surface)
        }
    }
}

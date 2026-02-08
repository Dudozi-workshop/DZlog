package com.example.dzlog.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [CaptureButtonSection]
 * - 목적: 촬영 화면 하단의 원형 촬영 버튼 UI를 분리함
 * - 포함: UI 렌더(활성/비활성 스타일) + 클릭 콜백 연결
 * - 제외: 촬영 저장/CameraX/Repository 로직 금지(호출은 상위에서 주입)
 */
@Composable
internal fun CaptureButtonSection(
    ready: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .background(
                color = if (ready) DDZColor.Surface else DDZColor.IconMuted,
                shape = CircleShape
            )
            // NOTE: 터치는 항상 받음(ready=false면 상위에서 토스트 처리)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (ready) DDZColor.PrimaryDark else DDZColor.Border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "●",
                color = if (ready) DDZColor.Surface else DDZColor.TextMuted,
                style = DDZTypography.CardTitle
            )
        }
    }
}

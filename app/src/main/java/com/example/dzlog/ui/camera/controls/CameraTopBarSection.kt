package com.example.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing

/**
 * [CameraTopBarSection]
 * - 목적: 촬영 화면 상단 네비게이션 UI(촬영 설정) 표시
 */
@Composable
internal fun CameraTopBarSection(
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
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                .clickable { onOpenSettings() }
                .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "촬영 설정",
                tint = DDZColor.Surface
            )
        }
    }
}

package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [TableEditSection]
 * - 목적: 표 편집 진입 UI
 */
@Composable
internal fun TableEditSection(
    onOpenTableEditor: () -> Unit
) {
    Spacer(Modifier.height(DDZSpacing.sectionGap))
    DDZButton(
        text = "표 편집",
        onClick = onOpenTableEditor,
        style = DDZButtonStyle.Primary
    )
    Text(
        text = "셀 속성/그룹/G1·G2 설정",
        color = DDZColor.Surface.copy(alpha = 0.7f),
        style = DDZTypography.Caption
    )
    Spacer(Modifier.height(DDZSpacing.screenPadding))
    Text(
        "표 위치/크기/스타일은 표 상세설정에서 변경",
        color = DDZColor.Surface.copy(alpha = 0.7f),
        style = DDZTypography.Caption
    )
    Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))
}

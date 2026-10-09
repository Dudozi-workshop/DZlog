package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSaveInfoSheet(
    fileName: String?,
    relativePath: String?,
    onOpenSaveSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DDZColor.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "다음 촬영 저장 정보",
                style = DDZTypography.SectionTitle,
                color = DDZColor.TextPrimary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "파일명",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
                Text(
                    text = fileName ?: "계산 중…",
                    style = DDZTypography.Body.copy(fontWeight = FontWeight.Medium),
                    color = DDZColor.TextPrimary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "저장경로",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
                Text(
                    text = relativePath ?: "계산 중…",
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                )
            }
            DDZButton(
                text = "저장 설정 열기",
                leadingIcon = Icons.Default.Settings,
                onClick = onOpenSaveSettings,
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

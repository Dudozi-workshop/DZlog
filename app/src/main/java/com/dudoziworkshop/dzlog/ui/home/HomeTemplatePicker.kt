package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTemplatePicker(
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DDZColor.Background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
            Text(
                text = "촬영 템플릿 선택",
                style = DDZTypography.SectionTitle,
                color = DDZColor.TextPrimary,
            )
            if (templates.isEmpty()) {
                Text(
                    text = "저장된 템플릿이 없습니다.",
                    style = DDZTypography.Body,
                    color = DDZColor.TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(templates, key = { it.id }) { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(template.id) }
                                .padding(vertical = 16.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = template.name,
                                style = DDZTypography.Body,
                                color = DDZColor.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (template.id == activeTemplateId) {
                                Text(
                                    text = "사용 중",
                                    style = DDZTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = DDZColor.Sage,
                                )
                            }
                        }
                        HorizontalDivider(color = DDZColor.Border)
                    }
                }
            }
            TextButton(
                onClick = onManage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("템플릿 관리", color = DDZColor.PrimaryDark)
            }
        }
    }
}

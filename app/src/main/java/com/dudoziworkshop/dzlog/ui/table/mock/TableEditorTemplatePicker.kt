package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Selection only: browsing or dismissing never changes the active camera template. */
@Composable
internal fun TableEditorTemplatePicker(
    templates: List<SavedTableTemplate>,
    currentTemplateId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    DDZBottomSheet(title = "템플릿 교체", onDismiss = onDismiss) {
        Text(
            "교체한 템플릿은 상단 저장을 누르면 촬영에 적용돼요.",
            color = DDZColor.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(templates.sortedByDescending { it.modifiedAt }, key = { it.id }) { template ->
                DDZCard(contentPadding = PaddingValues(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(template.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TemplateMiniPreview(template.templateState)
                        Column(Modifier.weight(1f)) {
                            Text(
                                template.name,
                                color = DDZColor.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (template.id == currentTemplateId) {
                            Icon(Icons.Default.Check, contentDescription = "편집 중",
                                tint = DDZColor.Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

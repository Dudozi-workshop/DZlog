package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.feature.table.policy.rotatingPhraseSetSummary
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorPhraseSetSheet(
    cellId: Int,
    phraseSets: List<RotatingPhraseSet>,
    selectedSetId: String?,
    onSelect: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember(cellId) { mutableStateOf(false) }
    var name by remember(cellId) { mutableStateOf("") }

    DDZBottomSheet(title = if (creating) "새 문구 세트" else "문구 세트", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (creating) {
                DDZButton(
                    text = "목록으로", leadingIcon = Icons.Filled.ArrowBack,
                    style = DDZButtonStyle.Text,
                    onClick = { creating = false; name = "" },
                )
                DDZTextField(
                    value = name, onValueChange = { name = it },
                    label = "세트 이름", modifier = Modifier.fillMaxWidth(),
                )
                DDZButton(
                    text = "추가", modifier = Modifier.fillMaxWidth(),
                    enabled = name.trim().isNotBlank(),
                    onClick = {
                        val newName = name.trim()
                        if (newName.isNotBlank()) {
                            onCreate(newName)
                            name = ""
                            creating = false
                        }
                    },
                )
            } else {
                if (phraseSets.isEmpty()) {
                    Text("등록된 문구 세트가 없습니다.", color = DDZColor.TextSecondary)
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DDZColor.Surface,
                        border = BorderStroke(1.dp, DDZColor.Border),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            phraseSets.forEachIndexed { index, set ->
                                val isSelected = set.id == selectedSetId
                                Surface(
                                    color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                            .padding(start = 12.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f).semantics { selected = isSelected },
                                            onClick = { onSelect(set.id) },
                                            color = androidx.compose.ui.graphics.Color.Transparent,
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        set.name,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = DDZColor.TextPrimary,
                                                    )
                                                    Text(
                                                        rotatingPhraseSetSummary(set),
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                        style = DDZTypography.Secondary,
                                                        color = DDZColor.TextSecondary,
                                                    )
                                                }
                                                if (isSelected) Icon(
                                                    Icons.Filled.Check, "선택됨",
                                                    tint = DDZColor.SelectedDark,
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onEdit(set.id) }, modifier = Modifier.size(44.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit, "문구 세트 편집",
                                                modifier = Modifier.size(18.dp),
                                                tint = DDZColor.TextSecondary,
                                            )
                                        }
                                    }
                                }
                                if (index != phraseSets.lastIndex) HorizontalDivider(
                                    thickness = 1.dp, color = DDZColor.Border,
                                )
                            }
                        }
                    }
                }
                DDZButton(
                    text = "새 문구 세트", leadingIcon = Icons.Filled.Add,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Secondary,
                    onClick = { name = ""; creating = true },
                )
                if (selectedSetId != null) {
                    DDZButton(
                        text = "선택 해제", modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Text,
                        onClick = { onSelect(null) },
                    )
                }
            }
        }
    }
}

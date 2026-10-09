package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
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
    DDZBottomSheet(
        title = if (creating) "새 문구 세트" else "문구 세트",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (creating) {
                DDZButton(
                    text = "목록으로",
                    leadingIcon = Icons.Filled.ArrowBack,
                    style = DDZButtonStyle.Text,
                    onClick = {
                        creating = false
                        name = ""
                    },
                )
                DDZTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "세트 이름",
                    modifier = Modifier.fillMaxWidth(),
                )
                DDZButton(
                    text = "추가",
                    modifier = Modifier.fillMaxWidth(),
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
                }
                phraseSets.forEach { set ->
                    val isSelected = set.id == selectedSetId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = { onSelect(set.id) },
                            modifier = Modifier.weight(1f).semantics { selected = isSelected },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
                            border = BorderStroke(1.dp, if (isSelected) DDZColor.SageBorder else DDZColor.Border),
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(set.name, color = DDZColor.TextPrimary)
                                    Text(
                                        "${set.items.size}개 문구",
                                        color = DDZColor.TextSecondary,
                                        style = DDZTypography.Secondary,
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = DDZColor.SelectedDark)
                                }
                            }
                        }
                        DDZButton(
                            text = "편집",
                            leadingIcon = Icons.Filled.Edit,
                            style = DDZButtonStyle.Text,
                            onClick = { onEdit(set.id) },
                        )
                    }
                }
                DDZButton(
                    text = "새 문구 세트",
                    leadingIcon = Icons.Filled.Add,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Secondary,
                    onClick = {
                        name = ""
                        creating = true
                    },
                )
                if (selectedSetId != null) {
                    DDZButton(
                        text = "선택 해제",
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Text,
                        onClick = { onSelect(null) },
                    )
                }
            }
        }
    }
}

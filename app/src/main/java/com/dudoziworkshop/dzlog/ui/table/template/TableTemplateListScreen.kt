package com.dudoziworkshop.dzlog.ui.table.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.TableTemplateSort
import com.dudoziworkshop.dzlog.data.template.sortTableTemplates
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.table.mock.TemplateMiniPreview
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableTemplateListScreen(
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    onBack: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onCreateTemplate: () -> Unit,
    onRenameTemplate: (String, String) -> Unit,
    onDuplicateTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
) {
    var menuTemplateId by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<SavedTableTemplate?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<SavedTableTemplate?>(null) }

    var sort by rememberSaveable { mutableStateOf(TableTemplateSort.MODIFIED) }
    var showSort by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(title = "표 템플릿", onBack = onBack, actions = {
                DDZButton(text = "새 템플릿", leadingIcon = Icons.Filled.Add,
                    minHeight = 40.dp, textStyleOverride = DDZTypography.Caption, onClick = onCreateTemplate)
            })
        },
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "저장된 템플릿이 없습니다.",
                        style = DDZTypography.Body,
                        color = DDZColor.TextMuted,
                    )
                    TextButton(onClick = onCreateTemplate) {
                        Text("새 템플릿 만들기")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "sort-controls") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("템플릿 ${templates.size}개", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        Box {
                            TextButton(onClick = { showSort = true }) {
                                Text(sort.label, style = DDZTypography.Caption, color = DDZColor.TextPrimary)
                                Icon(Icons.Filled.KeyboardArrowDown, null, tint = DDZColor.TextMuted, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                                TableTemplateSort.entries.forEach { option ->
                                    DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option; showSort = false })
                                }
                            }
                        }
                    }
                }
                items(
                    items = sortTableTemplates(templates, sort),
                    key = { it.id },
                ) { template ->
                    DDZCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTemplate(template.id) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TemplateMiniPreview(template.templateState)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = template.name,
                                        style = DDZTypography.CardTitle,
                                        color = DDZColor.TextPrimary,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (template.id == activeTemplateId) {
                                        Text(
                                            text = "사용 중",
                                            style = DDZTypography.Caption,
                                            color = DDZColor.SelectedDark,
                                            modifier = Modifier.background(DDZColor.SelectedSoft, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "${template.templateState.rows} × ${template.templateState.cols} · ${if (sort == TableTemplateSort.USED) formatTemplateDate(template.lastUsedAt, true) else formatTemplateDate(template.modifiedAt, false)}",
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextMuted,
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { menuTemplateId = template.id },
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "템플릿 메뉴",
                                        tint = DDZColor.TextMuted,
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuTemplateId == template.id,
                                    onDismissRequest = { menuTemplateId = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("이름 변경") },
                                        onClick = {
                                            menuTemplateId = null
                                            renameTarget = template
                                            renameDraft = template.name
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("복제") },
                                        onClick = {
                                            menuTemplateId = null
                                            onDuplicateTemplate(template.id)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("삭제") },
                                        onClick = {
                                            menuTemplateId = null
                                            deleteTarget = template
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("템플릿 이름 변경") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    label = { Text("템플릿 이름") },
                )
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = renameDraft.trim().isNotBlank(),
                    onClick = {
                        onRenameTemplate(target.id, renameDraft.trim())
                        renameTarget = null
                    },
                ) {
                    Text("저장")
                }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("템플릿을 삭제할까요?") },
            text = { Text("‘${target.name}’ 템플릿이 삭제됩니다.") },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTemplate(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text("삭제")
                }
            },
        )
    }
}

private fun formatTemplateDate(timestamp: Long, used: Boolean): String {
    if (timestamp <= 0L) return if (used) "사용 기록 없음" else "수정일 없음"
    return (if (used) "사용 " else "수정 ") + SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timestamp))
}
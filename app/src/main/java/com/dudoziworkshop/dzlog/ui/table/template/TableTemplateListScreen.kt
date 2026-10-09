package com.dudoziworkshop.dzlog.ui.table.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.ui.common.DDZCard
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

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "표 템플릿",
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.Primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DDZColor.Primary,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTemplate) {
                Icon(Icons.Filled.Add, contentDescription = "새 템플릿")
            }
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
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = templates.sortedByDescending { it.modifiedAt },
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
                                        maxLines = 1,
                                    )
                                    if (template.id == activeTemplateId) {
                                        Text(
                                            text = "사용 중",
                                            style = DDZTypography.Caption,
                                            color = DDZColor.Primary,
                                        )
                                    }
                                }
                                Text(
                                    text = "${template.templateState.rows} × ${template.templateState.cols} · ${formatModifiedAt(template.modifiedAt)}",
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

private fun formatModifiedAt(timestamp: Long): String {
    if (timestamp <= 0L) return "수정일 없음"
    return SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timestamp))
}

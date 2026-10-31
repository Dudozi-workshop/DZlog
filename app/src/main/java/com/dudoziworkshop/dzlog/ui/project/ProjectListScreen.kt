package com.dudoziworkshop.dzlog.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Non-selecting project list: tapping a row opens its detail, not the camera. */
@Composable
internal fun ProjectListScreen(
    projects: List<CaptureProject>,
    summaries: Map<String, ProjectCaptureSummary>,
    activeId: String?,
    createInitially: Boolean,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(createInitially) }
    val ongoing = remember(projects, summaries) {
        projects.filter(CaptureProject::isActive)
            .sortedByDescending { summaries[it.id]?.lastCapturedAtMillis ?: it.createdAt }
    }
    val completed = remember(projects, summaries) {
        projects.filterNot(CaptureProject::isActive)
            .sortedByDescending { summaries[it.id]?.lastCapturedAtMillis ?: it.createdAt }
    }
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("프로젝트", style = DDZTypography.ScreenTitle,
                color = DDZColor.TextPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = { showCreate = true }) { Text("+ 만들기") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 18.dp, vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                Text("진행 중 · " + ongoing.size, style = DDZTypography.SectionTitle,
                    color = DDZColor.TextPrimary, modifier = Modifier.padding(vertical = 10.dp))
            }
            if (ongoing.isEmpty()) item {
                Text("진행 중인 프로젝트가 없습니다.", color = DDZColor.TextSecondary,
                    style = DDZTypography.Secondary, modifier = Modifier.padding(12.dp))
            }
            items(ongoing, key = { "active-" + it.id }) { project ->
                ProjectListRow(project, summaries[project.id], project.id == activeId) {
                    onOpenProject(project.id)
                }
            }
            item {
                Text("완료 · " + completed.size, style = DDZTypography.SectionTitle,
                    color = DDZColor.TextPrimary,
                    modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            }
            if (completed.isEmpty()) item {
                Text("완료된 프로젝트가 없습니다.", color = DDZColor.TextSecondary,
                    style = DDZTypography.Secondary, modifier = Modifier.padding(12.dp))
            }
            items(completed, key = { "completed-" + it.id }) { project ->
                ProjectListRow(project, summaries[project.id], false) {
                    onOpenProject(project.id)
                }
            }
        }
    }
    if (showCreate) ProjectNameDialog(
        title = "새 프로젝트",
        confirmLabel = "만들기",
        onDismiss = { showCreate = false },
        onConfirm = { name ->
            showCreate = false
            onCreate(name)
        },
    )
}

@Composable
private fun ProjectListRow(
    project: CaptureProject,
    summary: ProjectCaptureSummary?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(DDZColor.Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            val count = summary?.captureCount ?: 0
            val date = summary?.lastCapturedAtMillis?.let(::formatProjectDate) ?: "촬영 없음"
            Text(count.toString() + "회 촬영 · " + date, style = DDZTypography.Caption,
                color = DDZColor.TextSecondary)
            if (selected) Text("현재 촬영 대상", style = DDZTypography.Caption,
                color = DDZColor.SelectedDark)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "상세 보기",
            tint = DDZColor.IconMuted)
    }
}

internal fun formatProjectDate(timestampMillis: Long): String =
    if (timestampMillis > 0L) {
        SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(timestampMillis))
    } else "촬영 없음"

@Composable
internal fun ProjectNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = DDZTypography.SectionTitle) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("프로젝트 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

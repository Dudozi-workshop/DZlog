package com.dudoziworkshop.dzlog.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Explicit selection only. New projects are created in the list, not here. */
@Composable
internal fun ProjectPickerDialog(
    projects: List<CaptureProject>,
    activeId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("촬영 프로젝트", style = DDZTypography.ScreenTitle) },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ProjectPickerRow("일반 촬영", activeId == null) { onSelect(null) }
                projects.filter(CaptureProject::isActive)
                    .sortedByDescending(CaptureProject::createdAt)
                    .forEach { project ->
                        ProjectPickerRow(project.title, project.id == activeId) {
                            onSelect(project.id)
                        }
                    }
                TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Text("+ 새 프로젝트 만들기")
                }
                TextButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                    Text("전체 프로젝트 관리")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}

@Composable
private fun ProjectPickerRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) DDZColor.PrimaryDark else DDZColor.TextPrimary,
            style = DDZTypography.Body,
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "현재 촬영 대상", tint = DDZColor.Selected)
        }
    }
}

package com.dudoziworkshop.dzlog.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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

/**
 * Choose the capture target without editing project metadata.
 * Creation and management remain in ProjectListScreen. The active selection
 * changes only through the caller's persisted project-selection callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectPickerSheet(
    projects: List<CaptureProject>,
    activeId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activeProjects = projects.filter(CaptureProject::isActive)
        .sortedByDescending(CaptureProject::createdAt)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DDZColor.Surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "촬영 프로젝트 선택",
                    modifier = Modifier.weight(1f),
                    style = DDZTypography.ScreenTitle,
                    color = DDZColor.TextPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 370.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "general") {
                    ProjectPickerChoice(
                        title = "일반 촬영",
                        selected = activeId == null,
                        onClick = { onSelect(null) },
                    )
                }
                item(key = "project-section") {
                    Text(
                        "진행 중인 프로젝트",
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                }
                if (activeProjects.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "진행 중인 프로젝트가 없습니다.",
                            modifier = Modifier.padding(12.dp),
                            style = DDZTypography.Secondary,
                            color = DDZColor.TextSecondary,
                        )
                    }
                }
                items(activeProjects, key = { it.id }) { project ->
                    ProjectPickerChoice(
                        title = project.title,
                        selected = project.id == activeId,
                        onClick = { onSelect(project.id) },
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onCreate) { Text("+ 새 프로젝트") }
                TextButton(onClick = onManage) { Text("전체 프로젝트 관리") }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProjectPickerChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) DDZColor.SurfaceSoft else DDZColor.Surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.SelectedDark)
            Spacer(Modifier.padding(horizontal = 5.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "현재 촬영 대상", tint = DDZColor.Selected)
            }
        }
    }
}

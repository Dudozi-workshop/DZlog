package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialCardBottomSpace
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
import com.dudoziworkshop.dzlog.ui.common.NextCaptureInfoFields
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private enum class CaptureInfoList { PROJECT, TEMPLATE }

/**
 * Camera preparation uses dedicated project/template choice pages within
 * the existing centre popup, without expanding lists beneath the cards.
 * During pending saves, callers must pass canChangeSelection=false. This view
 * still permits reading the project, template, filename and save paths.
 */
@Composable
internal fun CameraNextCaptureInfoPopup(
    nextFileName: String,
    relativePaths: List<String>,
    onOpenSaveSettings: () -> Unit,
    onDismiss: () -> Unit,
    projects: List<CaptureProject> = emptyList(),
    selectedProjectId: String? = null,
    templates: List<SavedTableTemplate> = emptyList(),
    selectedTemplateId: String? = null,
    canChangeSelection: Boolean = true,
    onSelectProject: (String?) -> Unit = {},
    onSelectTemplate: (String) -> Unit = {},
    onManageProjects: () -> Unit = {},
    onOpenTableEditor: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf<CaptureInfoList?>(null) }
    val projectName = projects.firstOrNull { it.id == selectedProjectId }?.title ?: "일반 촬영"
    val templateName = templates.firstOrNull { it.id == selectedTemplateId }?.name ?: "선택된 템플릿 없음"
    BackHandler { if (expanded != null) expanded = null else onDismiss() }
    Box(
        Modifier.fillMaxSize().padding(bottom = LocalTutorialCardBottomSpace.current),
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(DDZColor.PrimaryDark.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            modifier = Modifier
                .align(if (LocalTutorialReadOnly.current) Alignment.TopCenter else Alignment.Center)
                .then(if (LocalTutorialReadOnly.current) Modifier.statusBarsPadding() else Modifier)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .heightIn(max = 560.dp)
                .tutorialTarget("camera.next-info"),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.Card,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(when (expanded) {
                        CaptureInfoList.PROJECT -> "프로젝트 선택"
                        CaptureInfoList.TEMPLATE -> "템플릿 선택"
                        null -> "촬영 준비"
                    }, style = DDZTypography.OverlayTitleCompact, color = DDZColor.Primary)
                    if (expanded != null) {
                        IconButton(onClick = { expanded = null }, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "촬영 준비로 돌아가기")
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
                if (expanded == null) {
                    CaptureInfoSelectionHeader(
                        title = "프로젝트",
                        value = projectName,
                        icon = Icons.Default.Folder,
                        onClick = { expanded = CaptureInfoList.PROJECT },
                    )
                    CaptureInfoSelectionHeader(
                        title = "템플릿",
                        value = templateName,
                        icon = Icons.Default.GridView,
                        onClick = { expanded = CaptureInfoList.TEMPLATE },
                    )
                    if (!canChangeSelection && !LocalTutorialReadOnly.current) {
                        Text(
                            "저장·복원 또는 Undo 처리가 끝나면 프로젝트와 템플릿을 변경할 수 있어요.",
                            style = DDZTypography.Caption,
                            color = DDZColor.TextSecondary,
                        )
                    }
                    NextCaptureInfoFields(
                        fileName = nextFileName,
                        relativePaths = relativePaths,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onOpenTableEditor,
                            enabled = canChangeSelection,
                            modifier = Modifier.weight(1f),
                        ) { Text("표 상세설정") }
                        Button(
                            onClick = onOpenSaveSettings,
                            enabled = canChangeSelection,
                            modifier = Modifier.weight(1f),
                        ) { Text("저장 설정") }
                    }
                } else {
                    if (expanded == CaptureInfoList.PROJECT) {
                        Column {
                            CaptureInfoChoiceRow(
                                name = "일반 촬영",
                                selected = selectedProjectId == null,
                                enabled = canChangeSelection,
                                onClick = { expanded = null; onSelectProject(null) },
                            )
                            projects.filter(CaptureProject::isActive)
                                .sortedByDescending(CaptureProject::createdAt)
                                .forEach { project ->
                                    CaptureInfoChoiceRow(
                                        name = project.title,
                                        selected = selectedProjectId == project.id,
                                        enabled = canChangeSelection,
                                        onClick = { expanded = null; onSelectProject(project.id) },
                                    )
                                }
                            TextButton(
                                onClick = onManageProjects,
                                enabled = canChangeSelection,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("프로젝트 만들기 · 관리") }
                        }
                    }
                    if (expanded == CaptureInfoList.TEMPLATE) {
                        Column {
                            if (templates.isEmpty()) {
                                Text("사용 가능한 템플릿이 없습니다.", style = DDZTypography.Secondary,
                                    color = DDZColor.TextSecondary)
                            }
                            templates.forEach { template ->
                                CaptureInfoChoiceRow(
                                    name = template.name,
                                    selected = template.id == selectedTemplateId,
                                    enabled = canChangeSelection,
                                    onClick = { expanded = null; onSelectTemplate(template.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureInfoSelectionHeader(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = DDZColor.TextMuted, modifier = Modifier.size(16.dp))
        Text(title, style = DDZTypography.Caption, color = DDZColor.TextMuted)
    }
    Surface(
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(onClickLabel = title + " 선택", onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = DDZTypography.Body,
                color = DDZColor.TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DDZColor.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CaptureInfoChoiceRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled && !selected, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = DDZTypography.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) DDZColor.PrimaryDark else DDZColor.TextPrimary,
        )
        if (selected) Icon(Icons.Default.Check, contentDescription = "현재 선택",
            tint = DDZColor.SelectedDark)
    }
}

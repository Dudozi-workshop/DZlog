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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.NextCaptureInfoFields
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Camera information is a summary. Project and template selection use their existing screens. */
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
    onOpenProjectSelection: () -> Unit,
    onOpenTemplateSelection: () -> Unit,
    onOpenTableEditor: () -> Unit = {},
) {
    val projectName = projects.firstOrNull { it.id == selectedProjectId }?.title ?: "일반 촬영"
    val templateName = templates.firstOrNull { it.id == selectedTemplateId }?.name ?: "선택된 템플릿 없음"

    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().navigationBarsPadding()
            .padding(bottom = LocalTutorialCardBottomSpace.current),
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
                    Text("촬영 정보", style = DDZTypography.OverlayTitleCompact, color = DDZColor.Primary)
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
                CaptureInfoSelectionCard(
                    title = "프로젝트",
                    value = projectName,
                    icon = Icons.Default.Folder,
                    enabled = canChangeSelection,
                    onClick = onOpenProjectSelection,
                )
                CaptureInfoSelectionCard(
                    title = "템플릿",
                    value = templateName,
                    icon = Icons.Default.GridView,
                    enabled = canChangeSelection,
                    onClick = onOpenTemplateSelection,
                )
                if (!canChangeSelection && !LocalTutorialReadOnly.current) {
                    Text(
                        "저장·복원 또는 Undo 처리가 끝나면 프로젝트와 템플릿을 변경할 수 있어요.",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                }
                NextCaptureInfoFields(fileName = nextFileName, relativePaths = relativePaths)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DDZButton(
                        text = "표 상세설정",
                        leadingIcon = Icons.Default.Edit,
                        style = DDZButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        minHeight = 48.dp,
                        horizontalPadding = 9.dp,
                        leadingIconSize = 18.dp,
                        leadingIconSpacing = 6.dp,
                        enabled = canChangeSelection,
                        onClick = onOpenTableEditor,
                    )
                    DDZButton(
                        text = "저장 설정",
                        leadingIcon = Icons.Default.Settings,
                        style = DDZButtonStyle.Primary,
                        modifier = Modifier.weight(1f),
                        minHeight = 48.dp,
                        horizontalPadding = 9.dp,
                        leadingIconSize = 18.dp,
                        leadingIconSpacing = 6.dp,
                        enabled = canChangeSelection,
                        onClick = onOpenSaveSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureInfoSelectionCard(
    title: String,
    value: String,
    icon: ImageVector,
    enabled: Boolean,
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
                .clickable(enabled = enabled, onClickLabel = title + " 선택", onClick = onClick)
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

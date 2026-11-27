package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.NextCaptureInfoFields
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Camera information is a summary. Project and template selection use their existing screens. */
@Composable
internal fun IosCameraInfoDialog(
    nextFileName: String,
    relativePaths: List<String>,
    onOpenSaveSettings: () -> Unit,
    onDismiss: () -> Unit,
    projectName: String,
    templateName: String,
    canChangeSelection: Boolean,
    projectSelectionEnabled: Boolean,
    onOpenProjectSelection: () -> Unit,
    onOpenTemplateSelection: () -> Unit,
    onOpenTableEditor: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 360.dp)
                .fillMaxWidth().heightIn(max = 560.dp),
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
                    enabled = canChangeSelection && projectSelectionEnabled,
                    onClick = onOpenProjectSelection,
                )
                CaptureInfoSelectionCard(
                    title = "템플릿",
                    value = templateName,
                    icon = Icons.Default.GridView,
                    enabled = canChangeSelection,
                    onClick = onOpenTemplateSelection,
                )
                if (!canChangeSelection) {
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
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "$title 선택 · $value" }
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

package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/** UI only. File operations and SAF permissions remain in LogFolderScreen. */
@Composable
internal fun GalleryFolderManagementDialog(
    action: String,
    sourcePath: String,
    renameName: String,
    destinationPath: String,
    onRenameNameChange: (String) -> Unit,
    onChangeDestination: () -> Unit,
    impact: CapturePathImpact,
    confirmed: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    busy: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val rename = action == "rename"
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val proposed = runCatching {
        if (rename) GalleryFolderOperationPolicy.renamedPath(sourcePath, renameName)
        else GalleryFolderOperationPolicy.movedPath(sourcePath, destinationPath)
    }
    val impactMessage = when (impact) {
        CapturePathImpact.MATCH ->
            "현재 촬영 저장경로와 연결된 폴더입니다. 변경 후에도 카메라 저장설정은 그대로 유지됩니다."
        CapturePathImpact.POSSIBLE ->
            "날짜·표 값 등으로 만들어지는 촬영 경로에 영향을 줄 수 있습니다. 촬영 저장설정은 자동 변경되지 않습니다."
        CapturePathImpact.NONE ->
            "폴더 안의 결과·원본 사진과 모든 하위 폴더가 함께 변경됩니다. 촬영 저장설정은 유지됩니다."
    }

    DDZContentDialog(
        title = if (rename) "폴더 이름 변경" else "폴더 이동 최종 확인",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
            ) {
                Text(
                    if (rename) "변경할 이름과 영향을 확인해 주세요." else "이동할 폴더와 목적지를 확인해 주세요.",
                    style = typography.Body,
                    color = colors.TextSecondary,
                )
                GalleryFolderInfoCard(
                    name = sourcePath.trimEnd('/').substringAfterLast('/'),
                    detail = sourcePath.removePrefix(GalleryFolderIndexPolicy.ROOT).trimEnd('/'),
                )
                if (rename) {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = onRenameNameChange,
                        label = { Text("새 폴더 이름") },
                        enabled = !busy,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    GalleryFolderInfoCard(
                        name = "이동할 위치",
                        detail = destinationPath.removePrefix(GalleryFolderIndexPolicy.ROOT)
                            .trimEnd('/').ifBlank { "DZlog" },
                    )
                    TextButton(onClick = onChangeDestination, enabled = !busy) {
                        Text("다른 위치 선택")
                    }
                }
                proposed.exceptionOrNull()?.message?.let {
                    Text(it, style = typography.Caption, color = colors.Destructive)
                }
                Surface(color = colors.SurfaceSoft, shape = MaterialTheme.shapes.medium) {
                    Text(
                        impactMessage,
                        modifier = Modifier.fillMaxWidth().padding(spacing.controlGap),
                        style = typography.Secondary,
                        color = colors.TextSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = onConfirmedChange,
                        enabled = !busy && proposed.isSuccess,
                    )
                    Text(
                        "폴더와 촬영 저장경로에 미치는 영향을 확인했습니다.",
                        style = typography.Caption,
                        color = colors.TextPrimary,
                    )
                }
                error?.let { Text(it, style = typography.Secondary, color = colors.Destructive) }
            }
        },
        actions = {
            DDZButton(
                "취소",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary,
                enabled = !busy,
            )
            DDZButton(
                if (rename) "이름 변경" else "폴더 이동",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !busy && confirmed && proposed.isSuccess,
            )
        },
    )
}

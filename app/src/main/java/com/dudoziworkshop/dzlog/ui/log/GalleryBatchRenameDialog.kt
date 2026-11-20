package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryBatchRenamePolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
internal fun GalleryBatchRenameDialog(
    fileNames: List<String>,
    photo: Boolean,
    busy: Boolean,
    validate: (String, String) -> String?,
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val previews = runCatching {
        fileNames.map { old ->
            old to GalleryBatchRenamePolicy.renamedName(old, find, replace, photo)
        }.filter { (old, new) -> old != new }
    }
    val error = if (find.isEmpty()) "찾을 문구를 입력해 주세요."
        else previews.exceptionOrNull()?.message ?: validate(find, replace)
    val previewItems = previews.getOrDefault(emptyList())
    DDZContentDialog(
        title = "이름 일괄 변경",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                Text(if (photo) "선택한 사진의 지정 문구만 변경합니다. 확장자는 유지하며 연결된 원본도 함께 변경합니다."
                    else "선택한 폴더의 지정 문구만 변경합니다. 폴더 속 파일은 그대로 유지합니다.",
                    style = typography.Caption, color = colors.TextSecondary)
                OutlinedTextField(value = find, onValueChange = { find = it },
                    label = { Text("찾을 문구") }, singleLine = true, enabled = !busy,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = replace, onValueChange = { replace = it },
                    label = { Text("바꿀 문구 (비우면 삭제)") }, singleLine = true, enabled = !busy,
                    modifier = Modifier.fillMaxWidth())
                Text("변경 예정 ${previewItems.size}개", style = typography.Body)
                previewItems.take(5).forEach { (old, new) ->
                    GalleryPathPreviewCard("변경 전", old)
                    GalleryPathPreviewCard("변경 후", new)
                }
                if (previewItems.size > 5) Text("그 외 ${previewItems.size - 5}개 항목에도 동일하게 적용됩니다.",
                    style = typography.Caption, color = colors.TextSecondary)
                error?.let { Text(it, style = typography.Caption, color = colors.Destructive) }
            }
        },
        actions = {
            DDZButton("취소", onClick = onCancel, modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, enabled = !busy)
            DDZButton("일괄 변경", onClick = { onConfirm(find, replace) },
                modifier = Modifier.weight(1f),
                enabled = !busy && error == null && previewItems.isNotEmpty())
        },
    )
}

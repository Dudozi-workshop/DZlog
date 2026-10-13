package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
internal fun GalleryFolderInfoCard(name: String, detail: String) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    Surface(color = colors.SurfaceSoft, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(spacing.controlGap), horizontalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = colors.SelectedDark)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(name, style = typography.SettingLabel, color = colors.TextPrimary)
                Text(detail, style = typography.Caption, color = colors.TextSecondary)
            }
        }
    }
}

@Composable
internal fun GalleryFolderAccessGuide(
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    selectedPath: String? = null,
    showCancelAction: Boolean = true,
    required: Boolean = false,
    notice: String? = null,
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    DDZContentDialog(
        title = if (selectedPath == null) "DZlog 폴더를 연결해 주세요" else "DZlog 폴더를 다시 선택해 주세요",
        onDismiss = onCancel,
        properties = DialogProperties(
            dismissOnClickOutside = !required,
            dismissOnBackPress = true,
        ),
        content = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                Text(
                    if (required) "사진과 폴더를 안전하게 관리하려면 Pictures/DZlog 폴더 연결이 필요해요. 연결을 완료해야 DZlog를 사용할 수 있어요."
                    else "폴더 이름을 변경하거나 이동·삭제하려면 Android의 폴더 접근 허용이 필요해요.",
                    style = typography.Body,
                    color = colors.TextSecondary,
                )
                notice?.let {
                    Surface(color = colors.DestructiveSoft, shape = MaterialTheme.shapes.medium) {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(spacing.controlGap),
                            style = typography.Caption,
                            color = colors.Destructive,
                        )
                    }
                }
                selectedPath?.let { GalleryFolderInfoCard("선택한 폴더", it) }
                listOf(
                    "Pictures 폴더로 이동" to "Android 폴더 선택 화면에서 Pictures를 찾아 주세요.",
                    "DZlog 폴더 열기" to "하위 폴더가 아니라 Pictures / DZlog 폴더 자체를 열어 주세요.",
                    "‘이 폴더 사용’ 선택" to "기종에 따라 버튼 위치나 이름은 조금 다를 수 있어요.",
                ).forEachIndexed { index, (title, detail) ->
                    Surface(color = colors.SurfaceSoft, shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(spacing.controlGap), horizontalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                            Text("${index + 1}", style = typography.SettingLabel, color = colors.SelectedDark)
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text(title, style = typography.SettingLabel, color = colors.TextPrimary)
                                Text(detail, style = typography.Caption, color = colors.TextSecondary)
                            }
                        }
                    }
                }
                Text("DZlog 폴더가 아직 없다면 Pictures 안에서 새 폴더를 만들고 이름을 DZlog로 입력해 주세요.", style = typography.Caption, color = colors.TextSecondary)
                Text("연결만으로 사진이 변경되지는 않아요. 연결 권한이 유지되는 동안 이 안내는 생략돼요.", style = typography.Caption, color = colors.TextSecondary)
            }
        },
        actions = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                DDZButton(if (selectedPath == null && notice == null) "폴더 선택 화면으로" else "다시 연결하기", onSelect, Modifier.fillMaxWidth())
                if (showCancelAction) {
                    DDZButton("취소", onCancel, Modifier.fillMaxWidth(), style = DDZButtonStyle.Secondary)
                }
            }
        },
    )
}

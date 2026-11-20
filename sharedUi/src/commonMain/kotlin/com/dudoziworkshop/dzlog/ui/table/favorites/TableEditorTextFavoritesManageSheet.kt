package com.dudoziworkshop.dzlog.ui.table.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorTextFavoritesManageSheet(model: TextFavoritesState) {
    var editTarget by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }
    var removeTarget by remember { mutableStateOf<String?>(null) }
    val favorites = model.favorites

    DDZBottomSheet(title = "자주 쓰는 텍스트 관리", onDismiss = model::closeManagement) {
        Text(
            "등록 ${favorites.size}/${MAX_TEXT_FAVORITES} · 순서 변경은 화살표로 이동",
            style = DDZTypography.Caption,
            color = DDZColor.TextSecondary,
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            favorites.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { model.move(item, -1) },
                        enabled = index > 0,
                    ) { Icon(Icons.Filled.ArrowUpward, "위로 이동") }
                    IconButton(
                        onClick = { model.move(item, 1) },
                        enabled = index < favorites.lastIndex,
                    ) { Icon(Icons.Filled.ArrowDownward, "아래로 이동") }
                    IconButton(onClick = { editTarget = item; editValue = item }) {
                        Icon(Icons.Filled.Edit, "문구 수정")
                    }
                    IconButton(onClick = { removeTarget = item }) {
                        Icon(Icons.Filled.DeleteOutline, "문구 삭제")
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = DDZColor.Border)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = model::closeManagement) { Text("닫기") }
        }
    }

    editTarget?.let { old ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("즐겨찾기 문구 수정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("기존 셀에 입력된 텍스트는 변경되지 않습니다.", style = DDZTypography.Caption)
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("문구") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                    if (editValue.isNotBlank() && editValue != old && editValue in favorites) {
                        Text("이미 등록된 문구입니다.", color = DDZColor.TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editValue.isNotBlank() && (editValue == old || editValue !in favorites),
                    onClick = { if (model.rename(old, editValue)) editTarget = null },
                ) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("취소") } },
        )
    }

    removeTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("즐겨찾기에서 삭제할까요?") },
            text = { Text("‘${item}’을 목록에서만 제거합니다. 이미 입력된 셀 값은 유지됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    model.remove(item)
                    removeTarget = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("취소") } },
        )
    }
}

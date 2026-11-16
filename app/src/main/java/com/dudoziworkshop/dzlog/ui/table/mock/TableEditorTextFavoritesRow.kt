package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.table.TableTextFavoritesRepository
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorTextFavoritesViewModel
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.delay

/** Use the same star in table editing and camera quick editing. */
@Composable
internal fun TableEditorTextFavoriteStar(
    text: String,
    model: TableEditorTextFavoritesViewModel,
    enabled: Boolean = true,
) {
    val saved = text.isNotBlank() && text in model.favorites
    IconButton(
        onClick = { model.toggle(text) },
        enabled = enabled && text.isNotBlank(),
        modifier = Modifier,
    ) {
        Icon(
            if (saved) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (saved) "즐겨찾기 즉시 해제" else "즐겨찾기 즉시 등록",
            tint = if (saved) DDZColor.SelectionEmphasis else DDZColor.TextMuted,
            modifier = Modifier.padding(10.dp),
        )
    }
}

/** One fixed-height horizontally scrolling list; mutations never reorder on selection. */
@Composable
internal fun TableEditorTextFavoritesRow(
    model: TableEditorTextFavoritesViewModel,
    onApply: (String) -> Unit,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val favorites = model.favorites
    val change = model.lastChange
    LaunchedEffect(change?.serial) {
        if (change != null) {
            delay(4_500)
            model.clearUndo(change.serial)
        }
    }
    if (favorites.isNotEmpty() || change != null) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (change != null) {
                    Text(change.label, style = DDZTypography.Caption, modifier = Modifier.weight(1f))
                    TextButton(onClick = model::undo, enabled = enabled) { Text("되돌리기") }
                } else {
                    Text("자주 쓰는 텍스트", style = DDZTypography.Caption, modifier = Modifier.weight(1f))
                    TextButton(onClick = { model.openManagement() }, enabled = enabled) { Text("관리") }
                }
            }
            if (!compact && favorites.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    favorites.forEach { item ->
                        Surface(
                            onClick = { onApply(item) },
                            enabled = enabled,
                            shape = RoundedCornerShape(10.dp),
                            color = DDZColor.Card,
                        ) {
                            Text(
                                item,
                                style = DDZTypography.Secondary,
                                modifier = Modifier.widthIn(max = 180.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
    if (model.managementOpen) TableEditorTextFavoritesManageSheet(model)
    if (model.limitReached) {
        AlertDialog(
            onDismissRequest = model::dismissLimit,
            title = { Text("즐겨찾기 등록 한도") },
            text = { Text("최대 ${TableTextFavoritesRepository.MAX_ITEMS}개까지 등록할 수 있습니다. 기존 문구를 정리한 후 등록해주세요.") },
            confirmButton = {
                TextButton(onClick = { model.dismissLimit(); model.openManagement() }) { Text("목록 관리") }
            },
            dismissButton = { TextButton(onClick = model::dismissLimit) { Text("닫기") } },
        )
    }
}

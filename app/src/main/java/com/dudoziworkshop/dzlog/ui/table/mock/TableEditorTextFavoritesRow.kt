package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.table.TableTextFavoritesRepository
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorTextFavoritesViewModel
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

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

/**
 * One stable-height row shared by table detail and camera quick edit.
 * The full favorite is applied even when its chip label is truncated.
 * Only chips scroll; the management action stays pinned at the right.
 */
@Composable
internal fun TableEditorTextFavoritesRow(
    model: TableEditorTextFavoritesViewModel,
    onApply: (String) -> Unit,
    enabled: Boolean = true,
) {
    val favorites = model.favorites
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxWidth().height(DDZLayout.QuickInput.Touch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (favorites.isEmpty()) {
                Text(
                    "등록된 즐겨찾기가 없습니다",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    maxLines = 1,
                )
            }
            favorites.forEach { item ->
                Surface(
                    onClick = { onApply(item) },
                    enabled = enabled,
                    shape = RoundedCornerShape(9.dp),
                    color = DDZColor.Card,
                ) {
                    Text(
                        item,
                        style = DDZTypography.Secondary,
                        modifier = Modifier.widthIn(max = 140.dp)
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = DDZColor.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DDZColor.Border),
        ) {
            IconButton(
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    model.openManagement()
                },
                enabled = enabled,
                modifier = Modifier.size(DDZLayout.QuickInput.Touch),
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "즐겨찾기 관리")
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

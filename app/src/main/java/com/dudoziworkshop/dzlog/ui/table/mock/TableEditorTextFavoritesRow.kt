package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** The text input's star is the one-tap add/remove affordance. */
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
 * Exactly one fixed favorites row, visible while the keyboard is open.
 * Chips scroll horizontally; the fixed trailing ellipsis opens the existing
 * management sheet. No header, collapse button, or second-line undo banner.
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

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (favorites.isEmpty()) {
                Text(
                    "즐겨찾기 없음",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp),
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
                        color = DDZColor.TextPrimary,
                        modifier = Modifier.widthIn(max = 160.dp)
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        IconButton(
            onClick = {
                // Opening a modal should not leave the IME covering management.
                keyboard?.hide()
                focusManager.clearFocus()
                model.openManagement()
            },
            enabled = enabled,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Filled.MoreHoriz,
                contentDescription = "즐겨찾기 설정 및 관리",
                tint = DDZColor.TextSecondary,
            )
        }
    }
    if (model.managementOpen) TableEditorTextFavoritesManageSheet(model)
    if (model.limitReached) {
        AlertDialog(
            onDismissRequest = model::dismissLimit,
            title = { Text("즐겨찾기 등록 한도") },
            text = {
                Text(
                    "최대 ${TableTextFavoritesRepository.MAX_ITEMS}개까지 등록할 수 있습니다. 기존 문구를 정리한 후 등록해주세요."
                )
            },
            confirmButton = {
                TextButton(onClick = { model.dismissLimit(); model.openManagement() }) {
                    Text("목록 관리")
                }
            },
            dismissButton = {
                TextButton(onClick = model::dismissLimit) { Text("닫기") }
            },
        )
    }
}

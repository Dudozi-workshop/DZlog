package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorTextFavoritesRow(
    currentText: String,
    favorites: List<String>,
    onApply: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var managing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("자주 쓰는 텍스트", style = DDZTypography.Caption, modifier = Modifier.weight(1f))
            TextButton(onClick = onSave, enabled = currentText.isNotBlank()) { Text("현재 값 저장") }
            if (favorites.isNotEmpty()) {
                TextButton(onClick = { managing = true }) { Text("관리") }
            }
        }
        if (favorites.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                favorites.forEach { text ->
                    Surface(
                        onClick = { onApply(text) },
                        shape = RoundedCornerShape(10.dp),
                        color = DDZColor.Card,
                    ) {
                        Text(
                            text,
                            style = DDZTypography.Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (managing) {
        AlertDialog(
            onDismissRequest = { managing = false },
            title = { Text("자주 쓰는 텍스트 관리") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    favorites.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(Icons.Filled.DeleteOutline, "즐겨찾기 삭제")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { managing = false }) { Text("닫기") }
            },
        )
    }
}

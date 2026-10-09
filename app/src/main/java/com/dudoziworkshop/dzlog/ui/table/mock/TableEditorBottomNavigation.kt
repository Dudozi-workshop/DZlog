package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.ui.common.DDZBottomNavigation
import com.dudoziworkshop.dzlog.ui.common.DDZBottomNavigationItem

internal enum class MockBottomTab { CONTENT, STRUCTURE, STYLE, SAVE }

@Composable
internal fun MockBottomBar(
    active: MockBottomTab,
    onContent: () -> Unit,
    onLayout: () -> Unit,
    onStyle: () -> Unit,
    onSaveRules: () -> Unit,
) {
    DDZBottomNavigation(
        items = listOf(
            DDZBottomNavigationItem(
                label = "내용",
                icon = Icons.Filled.GridView,
                selected = active == MockBottomTab.CONTENT,
                onClick = onContent,
            ),
            DDZBottomNavigationItem(
                label = "구조",
                icon = Icons.Filled.ViewStream,
                selected = active == MockBottomTab.STRUCTURE,
                onClick = onLayout,
            ),
            DDZBottomNavigationItem(
                label = "스타일",
                icon = Icons.Filled.Palette,
                selected = active == MockBottomTab.STYLE,
                onClick = onStyle,
            ),
            DDZBottomNavigationItem(
                label = "저장설정",
                icon = Icons.Filled.Save,
                selected = active == MockBottomTab.SAVE,
                onClick = onSaveRules,
            ),
        ),
    )
}

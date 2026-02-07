package com.example.dzlog.ui.table.section

import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
fun TableEditorTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = DDZColor.Surface,
        contentColor = DDZColor.TextPrimary,
        modifier = modifier
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text("표 구조설정", style = DDZTypography.Body) },
            selectedContentColor = DDZColor.TextPrimary,
            unselectedContentColor = DDZColor.TextMuted
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text("표 미리보기", style = DDZTypography.Body) },
            selectedContentColor = DDZColor.TextPrimary,
            unselectedContentColor = DDZColor.TextMuted
        )
    }
}

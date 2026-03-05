package com.dudoziworkshop.dzlog.ui.table.section

import androidx.compose.foundation.layout.height
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

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
            modifier = Modifier.height(40.dp),
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text("표 구조설정", style = DDZTypography.Body) },
            selectedContentColor = DDZColor.TextPrimary,
            unselectedContentColor = DDZColor.TextMuted
        )
        Tab(
            modifier = Modifier.height(40.dp),
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text("표 미리보기", style = DDZTypography.Body) },
            selectedContentColor = DDZColor.TextPrimary,
            unselectedContentColor = DDZColor.TextMuted
        )
    }
}

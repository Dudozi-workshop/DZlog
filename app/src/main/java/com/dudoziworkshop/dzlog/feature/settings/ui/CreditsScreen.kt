package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val CARD_SHAPE = RoundedCornerShape(20.dp)

private val specialThanks = listOf("루루", "채채", "작은박")

@Composable
fun CreditsScreen(
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DDZColor.Background
    ) {
        Scaffold(
            containerColor = DDZColor.Background,
            topBar = { DDZTopBar(title = "도움 주신 분들", onBack = onBack) }
        ) { innerPadding ->
            // 상단 네비게이션은 고정하고 감사 명단만 스크롤한다.
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderCard()

                CreditsSectionCard(
                    icon = Icons.Default.Favorite,
                    title = "특별한 감사"
                ) {
                    Chips()
                }

            }
        }
    }
}

@Composable
private fun HeaderCard() {
    DDZCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        shape = CARD_SHAPE,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "DZLog 앱 아이콘",
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "DZlog",
                style = DDZTypography.ScreenTitle,
                color = DDZColor.PrimaryDark
            )
            Text(
                text = "함께해 주셔서 감사합니다",
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary
            )
        }
    }
}

@Composable
private fun CreditsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    DDZCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DDZColor.PrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = DDZTypography.Body,
                    color = DDZColor.PrimaryDark
                )
            }
            // 주요 정책: 슬롯 콘텐츠는 카드 본문 ColumnScope에서 직접 렌더링한다.
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        specialThanks.forEach { name ->
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = DDZColor.Background,
                border = BorderStroke(1.dp, DDZColor.Border)
            ) {
                Text(
                    text = name,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}


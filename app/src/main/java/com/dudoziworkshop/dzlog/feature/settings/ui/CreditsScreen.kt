package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val CARD_SHAPE = RoundedCornerShape(20.dp)

private val specialThanks = listOf("루루", "채채", "작은박")
private val supporters = listOf("아무개", "아무개", "아무개")
private val privateTesters = listOf("아무개", "아무개")
private val ideaFeedback = listOf("아무개", "아무개")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            topBar = { CreditsTopBar(onBack = onBack) },
            bottomBar = { SupportButton(onClick = {}) }
        ) { innerPadding ->
            // 주요 정책: 상/하단은 고정하고 중앙 콘텐츠만 스크롤되도록 유지한다.
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderCard()

                CreditsSectionCard(
                    icon = Icons.Default.Favorite,
                    title = "특별한 감사"
                ) {
                    Chips(names = specialThanks)
                }

                CreditsSectionCard(
                    icon = Icons.Default.Star,
                    title = "후원해주신 분들"
                ) {
                    ContributorLines(names = supporters)
                }

                CreditsSectionCard(
                    icon = Icons.Default.Build,
                    title = "앱 개발에 도움 주신 분들"
                ) {
                    ContributorGroup(
                        subtitle = "# 비공개 테스터",
                        names = privateTesters
                    )
                    ContributorGroup(
                        subtitle = "# 아이디어 및 피드백",
                        names = ideaFeedback
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditsTopBar(
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "도움 주신 분들",
                style = DDZTypography.CardTitle,
                color = DDZColor.Primary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = DDZColor.Primary
                )
            }
        },
        actions = { Spacer(modifier = Modifier.size(48.dp)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DDZColor.Background,
            navigationIconContentColor = DDZColor.Primary,
            titleContentColor = DDZColor.Primary,
            actionIconContentColor = DDZColor.Primary
        )
    )
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = DDZColor.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DDZColor.Border)
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
                text = "DZLog",
                style = DDZTypography.ScreenTitle,
                color = DDZColor.Primary
            )
            Text(
                text = "촬영·기록·정리를 한 번에",
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = DDZColor.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DDZColor.Border)
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
                    tint = DDZColor.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = DDZTypography.Body,
                    color = DDZColor.Primary
                )
            }
            // 주요 정책: 슬롯 콘텐츠는 카드 본문 ColumnScope에서 직접 렌더링한다.
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(names: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        names.forEach { name ->
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

@Composable
private fun ContributorLines(names: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEach { name ->
            Text(
                text = name,
                style = DDZTypography.Caption,
                color = DDZColor.TextPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ContributorGroup(
    subtitle: String,
    names: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = subtitle,
            style = DDZTypography.Body,
            color = DDZColor.Primary
        )
        ContributorLines(names = names)
    }
}

@Composable
private fun SupportButton(
    onClick: () -> Unit,
) {
    Surface(color = DDZColor.Background) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DDZColor.Primary)
        ) {
            Text(
                text = "후원하기",
                style = DDZTypography.ButtonText,
                color = Color.White
            )
        }
    }
}
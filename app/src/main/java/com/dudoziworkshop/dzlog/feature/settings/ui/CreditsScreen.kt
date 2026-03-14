package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val SPECIAL_THANKS = listOf("루루", "채채", "작은박")
private val PRIVATE_TESTERS = listOf("아무개", "아무개", "아무개")
private val IDEAS_AND_FEEDBACK = listOf("아무개", "아무개", "아무개")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = DDZSpacing.sectionGap)
                .padding(bottom = DDZSpacing.itemGap),
            verticalArrangement = Arrangement.spacedBy(DDZSpacing.sectionGap)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DDZColor.Card,
                border = BorderStroke(1.dp, DDZColor.Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DDZSpacing.cardPadding, vertical = 16.dp),
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
                        color = DDZColor.TextPrimary
                    )
                    Text(
                        text = "디지털의 영혼, 아날로그의 마음",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "특별한 감사",
                    style = DDZTypography.SectionTitle,
                    color = DDZColor.Primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DDZColor.Card,
                    border = BorderStroke(1.dp, DDZColor.Border)
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DDZSpacing.cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SPECIAL_THANKS.forEach { name ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = DDZColor.SageLight.copy(alpha = 0.32f),
                                border = BorderStroke(1.dp, DDZColor.Border)
                            ) {
                                Text(
                                    text = name,
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "앱 개발에 도움 주신 분들",
                    style = DDZTypography.SectionTitle,
                    color = DDZColor.Primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DDZColor.Card,
                    border = BorderStroke(1.dp, DDZColor.Border)
                ) {
                    Column(
                        modifier = Modifier.padding(DDZSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ContributorGroup(
                            subtitle = "# 비공개 테스터",
                            names = PRIVATE_TESTERS
                        )
                        ContributorGroup(
                            subtitle = "# 아이디어 및 피드백",
                            names = IDEAS_AND_FEEDBACK
                        )
                    }
                }
            }
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

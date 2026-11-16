package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val iosSpecialThanks = listOf("루루", "채채", "작은박")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IosCreditsSettingsScreen(versionLabel: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = "도움 주신 분들", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("DZlog", style = DDZTypography.ScreenTitle, color = DDZColor.PrimaryDark)
                    Text("함께해 주셔서 감사합니다", style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary)
                    Text(versionLabel, style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
            ) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null,
                        tint = DDZColor.PrimaryDark)
                    Text("특별한 감사", style = DDZTypography.Body, color = DDZColor.PrimaryDark)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        iosSpecialThanks.forEach { name ->
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = DDZColor.Background,
                                border = BorderStroke(1.dp, DDZColor.Border),
                            ) {
                                Text(name, style = DDZTypography.Caption, color = DDZColor.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

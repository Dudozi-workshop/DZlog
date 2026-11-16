package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** iOS exposes app haptics while leaving mandatory/system shutter sound policy to iPhone. */
@Composable
internal fun IosCaptureFeedbackSettingsScreen(
    hapticEnabled: Boolean,
    onBack: () -> Unit,
    onChangeHaptic: (Boolean) -> Boolean,
) {
    val feedback = remember { IosCaptureFeedback() }
    var saveFailed by remember { mutableStateOf(false) }

    fun changeHaptic(next: Boolean) {
        saveFailed = !onChangeHaptic(next)
        if (!saveFailed && next) {
            feedback.prepare(true)
            feedback.play(true)
        }
    }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = { DDZTopBar(title = "촬영 효과음 및 진동", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("촬영 진동", style = DDZTypography.ScreenTitle, color = DDZColor.PrimaryDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (hapticEnabled) "촬영 진동: 켜짐" else "촬영 진동: 꺼짐",
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
                    )
                    Text("촬영 요청이 접수될 때 진동합니다.", style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary)
                }
                Switch(checked = hapticEnabled, onCheckedChange = ::changeHaptic)
            }
            TextButton(onClick = { changeHaptic(!hapticEnabled) }) {
                Text(if (hapticEnabled) "촬영 진동 끄기" else "촬영 진동 켜기")
            }
            Button(enabled = hapticEnabled, onClick = {
                feedback.prepare(true)
                feedback.play(true)
            }) { Text("진동 미리보기") }
            if (saveFailed) Text("진동 설정을 저장하지 못했습니다. 다시 시도해 주세요.",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)

            HorizontalDivider(color = DDZColor.Border)
            Text("촬영음", style = DDZTypography.ScreenTitle, color = DDZColor.PrimaryDark)
            Text(
                "촬영음은 iPhone의 시스템 설정과 지역 정책을 따릅니다. " +
                    "DZlog가 시스템 촬영음을 임의로 끄거나 음량을 변경하지 않습니다.",
                style = DDZTypography.Body,
                color = DDZColor.TextSecondary,
            )
        }
    }
}

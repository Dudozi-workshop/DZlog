package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.DzlogTutorialSections
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Camera preview is visible behind this passive tour; no input or capture is required. */
@Composable
internal fun LiveCameraTutorial(sessionId: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val session = DzlogTutorialSections.area("camera")?.sessions?.firstOrNull { it.id == sessionId }
        ?: return
    val pages = remember(sessionId) {
        session.steps.flatMap { step ->
            listOf(step.title to step.instruction) + step.scenes.map { it.title to it.instruction }
        }
    }
    var index by remember(sessionId) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(pages.indices)
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("카메라 · ${session.title} · ${safeIndex + 1}/${pages.size}",
                    modifier = Modifier.weight(1f),
                    style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                TextButton(onClick = onClose) { Text("닫기") }
            }
            Text(pages[safeIndex].first, style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark)
            Text(pages[safeIndex].second, style = DDZTypography.Body, color = DDZColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { index-- }, enabled = safeIndex > 0, modifier = Modifier.weight(1f)) {
                    Text("이전")
                }
                Button(
                    onClick = { if (safeIndex == pages.lastIndex) onClose() else index++ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                ) {
                    Text(if (safeIndex == pages.lastIndex) "마치기" else "다음")
                }
            }
        }
    }
}

package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Lets users browse every starter without pushing the photo preview below
 * the fold. Selection remains local to the parent onboarding draft.
 */
@Composable
internal fun OnboardingStarterAlternativeDialog(
    category: String,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val alternatives = OnboardingStarterCatalog.definitions
        .filter { it.category == category }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시작 양식 선택", color = DDZColor.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "양식을 선택하면 아래 미리보기가 바뀌어요.",
                    color = DDZColor.TextSecondary,
                )
                alternatives.forEach { def ->
                    val selected = def.id == selectedId
                    Text(
                        def.title,
                        color = if (selected) DDZColor.SelectedDark else DDZColor.TextPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                if (selected) DDZColor.SelectedSoft else DDZColor.SurfaceSoft,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelect(def.id) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = DDZColor.PrimaryDark) }
        },
        containerColor = DDZColor.Surface,
    )
}

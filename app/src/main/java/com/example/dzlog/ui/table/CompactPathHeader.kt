package com.example.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dzlog.R
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
internal fun CompactPathHeader(
    savePath: String,
    fileName: String,
    counterModeLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("저장경로", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(
            savePath,
            style = DDZTypography.Body,
            color = DDZColor.TextPrimary,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_filename), style = DDZTypography.Caption, color = DDZColor.TextMuted)
            Text(counterModeLabel, style = DDZTypography.Caption, color = DDZColor.TextMuted)
        }
        Text(
            fileName,
            style = DDZTypography.Body,
            color = DDZColor.TextPrimary,
            maxLines = 2
        )
    }
}

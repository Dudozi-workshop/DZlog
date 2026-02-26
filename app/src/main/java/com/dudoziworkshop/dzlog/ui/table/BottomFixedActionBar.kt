package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun BottomFixedActionBar(
    rows: Int,
    cols: Int,
    isSaving: Boolean,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = onAddRow) {
                Text("행 +", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onRemoveRow, enabled = rows > 1) {
                Text("행 -", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onAddCol) {
                Text("열 +", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onRemoveCol, enabled = cols > 1) {
                Text("열 -", style = DDZTypography.ButtonText)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = onReset) {
                Text("초기화", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("저장 중...", style = DDZTypography.ButtonText)
                } else {
                    Text("저장", style = DDZTypography.ButtonText)
                }
            }
        }
    }
}

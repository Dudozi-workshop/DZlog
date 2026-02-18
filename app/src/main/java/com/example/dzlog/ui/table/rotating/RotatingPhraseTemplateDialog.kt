package com.example.dzlog.ui.table.rotating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dzlog.domain.model.RotatingPhraseSet
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
fun RotatingPhraseTemplateDialog(
    cell: TableCellState,
    phraseSets: List<RotatingPhraseSet>,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSelectSet: (String?) -> Unit,
    onEveryChange: (Int) -> Unit,
    onIncreaseEvery: () -> Unit,
    onDecreaseEvery: () -> Unit,
    onRequestCreateSet: () -> Unit,
    onRequestDeleteSet: (String) -> Unit,
    onRequestEditSet: (String) -> Unit
) {
    val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
    val everyEnabled = selectedSet != null
    val everyDisplay = (cell.everyOverride ?: selectedSet?.defaultEvery ?: 1).coerceAtLeast(1)
    val listState = rememberLazyListState()
    var everyInput by remember(cell.cellId, cell.phraseSetId, cell.everyOverride, selectedSet?.defaultEvery) {
        mutableStateOf(everyDisplay.toString())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.PrimaryBrown
        ) {
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "문구 템플릿 설정",
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = DDZTypography.CardTitle,
                    color = DDZColor.TextPrimary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp)
                        .background(DDZColor.Card, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(items = phraseSets, key = { it.id }) { set ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSet(set.id) }
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = cell.phraseSetId == set.id,
                                    onClick = { onSelectSet(set.id) }
                                )
                                Text(
                                    text = "${set.name} (${set.items.size}개)",
                                    modifier = Modifier.weight(1f),
                                    color = DDZColor.TextPrimary,
                                    style = DDZTypography.Body
                                )
                                TextButton(onClick = { onRequestEditSet(set.id) }) { Text("✏️") }
                                TextButton(onClick = { onRequestDeleteSet(set.id) }) { Text("🗑") }
                            }
                        }
                    }

                    if (phraseSets.isEmpty()) {
                        Text(
                            text = "항목을 추가해주세요.",
                            modifier = Modifier.align(Alignment.Center),
                            color = DDZColor.TextMuted,
                            style = DDZTypography.Body
                        )
                    }

                    RotatingScrollIndicator(
                        listState = listState,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRequestCreateSet
                ) {
                    Text("+ 새 템플릿 추가", style = DDZTypography.ButtonText)
                }

                Spacer(modifier = Modifier.height(11.dp))

                Text(
                    text = "N장마다 다음 문구로 변경",
                    color = DDZColor.SageLight,
                    style = DDZTypography.Caption
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DDZColor.Sage,
                            contentColor = Color.Black
                        ),
                        onClick = onDecreaseEvery,
                        enabled = everyEnabled
                    ) { Text("-") }
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        value = everyInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            everyInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null) {
                                val clamped = parsed.coerceAtLeast(1)
                                onEveryChange(clamped)
                                if (clamped.toString() != digits) {
                                    everyInput = clamped.toString()
                                }
                            }
                        },
                        enabled = everyEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = DDZTypography.Body.copy(lineHeight = 20.sp)
                    )
                    Button(
                        modifier = Modifier.height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DDZColor.Sage,
                            contentColor = Color.Black
                        ),
                        onClick = onIncreaseEvery,
                        enabled = everyEnabled
                    ) { Text("+") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRestore) {
                        Text("복구", style = DDZTypography.ButtonText)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("닫기", style = DDZTypography.ButtonText)
                    }
                }
            }
        }
    }
}

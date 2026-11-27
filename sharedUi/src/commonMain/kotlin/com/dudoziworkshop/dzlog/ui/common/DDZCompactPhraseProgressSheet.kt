package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

/**
 * Shared compact phrase control for camera quick edit and table detail.
 * Phrase selection/progress and period changes are delegated to the screen's
 * existing template policies, including shared-set conflict resolution.
 */
@Composable
fun DDZCompactPhraseProgressSheet(
    cellKey: String,
    set: RotatingPhraseSet,
    phraseIndex: Int,
    completedShots: Int,
    legacyEveryOverride: Int?,
    onProgressChange: (Int, Int) -> Unit,
    onPeriodChange: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (set.items.isEmpty()) return
    val index = phraseIndex.coerceIn(set.items.indices)
    val override = set.itemEveryOverrides.getOrNull(index)
    val useSet = override == null && legacyEveryOverride == null
    val individualEvery = override
        ?: legacyEveryOverride
        ?: set.itemEveryInactiveOverrides.getOrNull(index)
        ?: set.defaultEvery
    val currentEvery = (override ?: legacyEveryOverride ?: set.defaultEvery).coerceAtLeast(1)
    val currentShots = completedShots.coerceIn(0, currentEvery - 1)
    var sliderDraft by remember(cellKey, set.id, index) { mutableFloatStateOf(currentShots.toFloat()) }
    LaunchedEffect(currentShots, currentEvery) {
        sliderDraft = currentShots.toFloat()
    }
    val maxShots = (currentEvery - 1).coerceAtLeast(0)
    val editEvery = if (useSet) set.defaultEvery else individualEvery
    // The number itself is editable; changing it never opens another dialog.
    var periodText by remember(cellKey, set.id, index, useSet, editEvery) {
        mutableStateOf(editEvery.toString())
    }
    var periodFocused by remember(cellKey, set.id, index, useSet) { mutableStateOf(false) }
    var inputInvalid by remember(cellKey, set.id, index, useSet) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun commitPeriodInput() {
        val typed = periodText.toIntOrNull()
        if (typed == null || typed !in 1..RotatingPhraseSet.MAX_EVERY) {
            periodText = editEvery.toString()
            inputInvalid = true
        } else {
            inputInvalid = false
            if (typed != editEvery) onPeriodChange(useSet, typed)
        }
    }
    val maxRows = minOf(5, set.items.size)
    BoxWithConstraints {
        val listHeight = minOf(40.dp * maxRows, maxHeight * 0.35f)

        DDZBottomSheet(title = "문구 진행 상태", onDismiss = onDismiss) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DDZColor.Border),
                color = DDZColor.Surface,
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(listHeight)) {
                    itemsIndexed(set.items) { i, text ->
                        val selected = i == index
                        val rowEvery = (
                            set.itemEveryOverrides.getOrNull(i)
                                ?: legacyEveryOverride
                                ?: set.defaultEvery
                            ).coerceAtLeast(1)
                        Surface(
                            onClick = {
                                if (!selected) onProgressChange(i, 0)
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            color = if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    (i + 1).toString().padStart(2, '0'),
                                    modifier = Modifier.width(28.dp),
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextSecondary,
                                )
                                Text(
                                    text, modifier = Modifier.weight(1f),
                                    style = DDZTypography.Body,
                                    color = DDZColor.TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${rowEvery}장", style = DDZTypography.Secondary,
                                    color = DDZColor.TextSecondary,
                                )
                                Text(
                                    if (selected) "✓" else " ",
                                    modifier = Modifier.width(18.dp),
                                    style = DDZTypography.Body,
                                    color = DDZColor.SelectionEmphasis,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                        if (i < set.items.lastIndex) HorizontalDivider(color = DDZColor.Border)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("완료 장수", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text("${sliderDraft.roundToInt().coerceIn(0, maxShots)} / ${currentEvery}장",
                    style = DDZTypography.Body, color = DDZColor.TextPrimary)
            }
            Slider(
                value = sliderDraft.coerceIn(0f, maxShots.coerceAtLeast(1).toFloat()),
                onValueChange = { sliderDraft = it.coerceIn(0f, maxShots.toFloat()) },
                onValueChangeFinished = {
                    val next = sliderDraft.roundToInt().coerceIn(0, maxShots)
                    if (next != currentShots) onProgressChange(index, next)
                },
                valueRange = 0f..maxShots.coerceAtLeast(1).toFloat(),
                steps = if (currentEvery in 2..20) currentEvery - 2 else 0,
                enabled = currentEvery > 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DDZColor.Border),
                color = DDZColor.Surface,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    PeriodScope(
                        title = "전체", every = set.defaultEvery, selected = useSet,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!useSet) onPeriodChange(true, set.defaultEvery) },
                    )
                    DDZPeriodStep(
                        symbol = "−", enabled = editEvery > 1,
                        onClick = { onPeriodChange(useSet, (editEvery - 1).coerceAtLeast(1)) },
                    )
                    Box(modifier = Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                        BasicTextField(
                            value = periodText,
                            onValueChange = { text ->
                                if (text.length <= 6 && text.all(Char::isDigit)) {
                                    periodText = text
                                    inputInvalid = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { focus ->
                                if (periodFocused && !focus.isFocused) commitPeriodInput()
                                periodFocused = focus.isFocused
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                            }),
                            textStyle = DDZTypography.Body.copy(
                                color = if (inputInvalid) DDZColor.Destructive else DDZColor.TextPrimary,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                    DDZPeriodStep(
                        symbol = "+", enabled = editEvery < RotatingPhraseSet.MAX_EVERY,
                        onClick = { onPeriodChange(useSet, editEvery + 1) },
                    )
                    PeriodScope(
                        title = "개별", every = individualEvery, selected = !useSet,
                        modifier = Modifier.weight(1f),
                        onClick = { if (useSet) onPeriodChange(false, individualEvery) },
                    )
                }
            }
            DDZButton(
                text = "완료",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun PeriodScope(
    title: String,
    every: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(
                if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = DDZTypography.Secondary,
            color = if (selected) DDZColor.SelectionEmphasis else DDZColor.TextSecondary)
        Text("(${every}장)", style = DDZTypography.Caption,
            color = if (selected) DDZColor.SelectionEmphasis else DDZColor.TextSecondary)
    }
}

@Composable
private fun DDZPeriodStep(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(32.dp).height(44.dp),
        color = DDZColor.Surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = DDZTypography.Body,
                color = if (enabled) DDZColor.TextPrimary else DDZColor.TextMuted)
        }
    }
}

package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

internal enum class MockRuleSourceType(val label: String) {
    CELL("셀에서 가져오기"),
    MANUAL("직접 입력"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

internal data class MockRuleItem(
    val sourceType: MockRuleSourceType,
    val value: String,
    val cellId: String? = null,
)

internal data class MockSaveRulesDraft(
    val fileNameItems: List<MockRuleItem?>,
    val pathItems: List<MockRuleItem?>,
    val includePathInScope: Boolean,
    val includeFilenameInScope: Boolean,
)

internal fun defaultMockSaveRulesDraft(): MockSaveRulesDraft =
    MockSaveRulesDraft(
        fileNameItems = listOf(
            MockRuleItem(MockRuleSourceType.CELL, "Draper", cellId = "mock-0"),
            MockRuleItem(MockRuleSourceType.DATE, "20261006"),
            null,
        ),
        pathItems = listOf(
            MockRuleItem(MockRuleSourceType.CELL, "Draper", cellId = "mock-0"),
            MockRuleItem(MockRuleSourceType.MANUAL, "처리구 A"),
            MockRuleItem(MockRuleSourceType.DATE, "20261006"),
        ),
        includePathInScope = true,
        includeFilenameInScope = true,
    )

private enum class MockRuleSection {
    FILE_NAME,
    PATH,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MockSaveRulesSheet(
    cells: List<MockCell>,
    rows: Int,
    cols: Int,
    draft: MockSaveRulesDraft,
    onDraftChange: (MockSaveRulesDraft) -> Unit,
    onApply: (MockSaveRulesDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingSection by remember { mutableStateOf<MockRuleSection?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showCellPicker by remember { mutableStateOf(false) }
    var showManualEditor by remember { mutableStateOf(false) }
    var manualDraft by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    val fileNamePrefix = draft.fileNameItems
        .mapNotNull { it?.value?.trim()?.takeIf(String::isNotBlank) }
        .joinToString("_")
    val fileNamePreview = buildString {
        append(if (fileNamePrefix.isBlank()) "DZlog" else fileNamePrefix)
        append("_0012.jpg")
    }
    val pathPreview = draft.pathItems
        .mapNotNull { it?.value?.trim()?.takeIf(String::isNotBlank) }
        .joinToString(" / ")

    fun activeItems(): List<MockRuleItem?> =
        when (editingSection) {
            MockRuleSection.FILE_NAME -> draft.fileNameItems
            MockRuleSection.PATH -> draft.pathItems
            null -> emptyList()
        }

    fun updateActiveItems(items: List<MockRuleItem?>) {
        onDraftChange(
            when (editingSection) {
                MockRuleSection.FILE_NAME -> draft.copy(fileNameItems = items)
                MockRuleSection.PATH -> draft.copy(pathItems = items)
                null -> draft
            }
        )
    }

    fun closeEditor() {
        editingSection = null
        editingIndex = null
        showCellPicker = false
        showManualEditor = false
        manualDraft = ""
    }

    fun openEditor(section: MockRuleSection, index: Int) {
        editingSection = section
        editingIndex = index
        showCellPicker = false
        showManualEditor = false
        manualDraft = when (section) {
            MockRuleSection.FILE_NAME -> draft.fileNameItems.getOrNull(index)
            MockRuleSection.PATH -> draft.pathItems.getOrNull(index)
        }?.takeIf { it.sourceType == MockRuleSourceType.MANUAL }?.value.orEmpty()
    }

    fun setItem(item: MockRuleItem) {
        val index = editingIndex ?: return
        val updated = activeItems().toMutableList().also {
            while (it.size < 3) it += null
            it[index] = item
        }.take(3)
        updateActiveItems(updated)
        closeEditor()
    }

    fun removeItem() {
        val index = editingIndex ?: return
        val compacted = mutableListOf<MockRuleItem?>().apply {
            addAll(activeItems().filterNotNull())
        }
        if (index in compacted.indices) compacted.removeAt(index)
        while (compacted.size < 3) compacted += null
        updateActiveItems(compacted.take(3))
        closeEditor()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                editingSection == null -> {
                    Text("저장 규칙", fontWeight = FontWeight.Bold)

                    CompactRuleSummarySection(
                        title = "파일명",
                        items = draft.fileNameItems,
                        emptyLabel = { index -> "+ ${index + 1}항목" },
                        onItemClick = { index -> openEditor(MockRuleSection.FILE_NAME, index) },
                    )
                    Text(
                        fileNamePreview,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        "자동번호 0012는 항상 마지막에 붙습니다.",
                        color = DDZColor.TextMuted,
                    )

                    CompactRuleSummarySection(
                        title = "저장경로",
                        items = draft.pathItems,
                        emptyLabel = { index -> "+ ${index + 1}단계" },
                        onItemClick = { index -> openEditor(MockRuleSection.PATH, index) },
                    )
                    Text(
                        text = if (pathPreview.isBlank()) {
                            "Pictures/DZlog/"
                        } else {
                            "Pictures/DZlog/${pathPreview.replace(" / ", "/")}/"
                        },
                        color = DDZColor.TextMuted,
                        maxLines = 1,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("자동번호", color = DDZColor.TextMuted)
                            Text("다음 번호 0012", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAdvanced = !showAdvanced },
                    ) {
                        Text(if (showAdvanced) "고급 설정 접기" else "고급 설정")
                    }

                    if (showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("자동번호 범위", color = DDZColor.TextMuted)
                            ScopeToggle(
                                label = "저장경로 변경 시 번호 분리",
                                checked = draft.includePathInScope,
                                onCheckedChange = {
                                    onDraftChange(draft.copy(includePathInScope = it))
                                },
                            )
                            ScopeToggle(
                                label = "파일명 구성 변경 시 번호 분리",
                                checked = draft.includeFilenameInScope,
                                onCheckedChange = {
                                    onDraftChange(draft.copy(includeFilenameInScope = it))
                                },
                            )
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        onClick = { onApply(draft) },
                    ) {
                        Text("적용")
                    }
                }

                showCellPicker -> {
                    val section = editingSection ?: return@Column
                    val index = editingIndex ?: return@Column
                    val title = when (section) {
                        MockRuleSection.FILE_NAME -> "${index + 1}항목 · 셀에서 가져오기"
                        MockRuleSection.PATH -> "${index + 1}단계 · 셀에서 가져오기"
                    }
                    Text(title, fontWeight = FontWeight.Bold)
                    Text("사용할 셀을 눌러주세요.", color = DDZColor.TextMuted)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
                    ) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until cols) {
                                    val cellIndex = row * cols + col
                                    val cell = cells.getOrNull(cellIndex)
                                    val isBlockedCounter =
                                        section == MockRuleSection.FILE_NAME &&
                                            cell?.type == MockCellType.COUNTER
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                            .border(0.5.dp, DDZColor.Border)
                                            .clickable(enabled = cell != null && !isBlockedCounter) {
                                                if (cell != null && !isBlockedCounter) {
                                                    setItem(
                                                        MockRuleItem(
                                                            sourceType = MockRuleSourceType.CELL,
                                                            value = cell.value,
                                                            cellId = cell.domainCellId,
                                                        )
                                                    )
                                                }
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cell?.value ?: "",
                                                color = if (cell == null || isBlockedCounter) {
                                                    DDZColor.TextMuted
                                                } else {
                                                    DDZColor.TextPrimary
                                                },
                                                maxLines = 2,
                                            )
                                            if (isBlockedCounter) {
                                                Text(
                                                    "자동번호",
                                                    color = DDZColor.TextMuted,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (section == MockRuleSection.FILE_NAME) {
                        Text(
                            "자동번호 셀은 선택하지 않아도 파일명 끝에 자동으로 붙습니다.",
                            color = DDZColor.TextMuted,
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCellPicker = false },
                    ) {
                        Text("다른 방식 선택")
                    }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::closeEditor,
                    ) {
                        Text("취소")
                    }
                }

                showManualEditor -> {
                    val section = editingSection ?: return@Column
                    val index = editingIndex ?: return@Column
                    val title = when (section) {
                        MockRuleSection.FILE_NAME -> "${index + 1}항목 · 직접 입력"
                        MockRuleSection.PATH -> "${index + 1}단계 · 직접 입력"
                    }
                    Text(title, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = manualDraft,
                        onValueChange = { manualDraft = it },
                        label = {
                            Text(
                                if (section == MockRuleSection.FILE_NAME) {
                                    "파일명 항목"
                                } else {
                                    "폴더명"
                                }
                            )
                        },
                        singleLine = true,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualDraft.trim().isNotBlank(),
                        onClick = {
                            setItem(
                                MockRuleItem(
                                    sourceType = MockRuleSourceType.MANUAL,
                                    value = manualDraft.trim(),
                                )
                            )
                        },
                    ) {
                        Text("적용")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showManualEditor = false },
                    ) {
                        Text("다른 방식 선택")
                    }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::closeEditor,
                    ) {
                        Text("취소")
                    }
                }

                else -> {
                    val section = editingSection ?: return@Column
                    val index = editingIndex ?: return@Column
                    val title = when (section) {
                        MockRuleSection.FILE_NAME -> "${index + 1}항목 설정"
                        MockRuleSection.PATH -> "${index + 1}단계 설정"
                    }
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        if (section == MockRuleSection.FILE_NAME) {
                            "파일명에 어떤 값을 넣을까요?"
                        } else {
                            "어떤 값을 폴더명으로 사용할까요?"
                        },
                        color = DDZColor.TextMuted,
                    )

                    MockRuleSourceType.entries.forEach { source ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                when (source) {
                                    MockRuleSourceType.CELL -> {
                                        showCellPicker = true
                                        showManualEditor = false
                                    }
                                    MockRuleSourceType.MANUAL -> {
                                        showManualEditor = true
                                        showCellPicker = false
                                    }
                                    MockRuleSourceType.DATE -> setItem(
                                        MockRuleItem(source, "20261006")
                                    )
                                    MockRuleSourceType.TIME -> setItem(
                                        MockRuleItem(source, "1148")
                                    )
                                    MockRuleSourceType.ROTATING_TEXT -> setItem(
                                        MockRuleItem(source, "품종")
                                    )
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(source.label)
                                if (activeItems().getOrNull(index)?.sourceType == source) {
                                    Text("✓")
                                }
                            }
                        }
                    }

                    if (activeItems().getOrNull(index) != null) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = ::removeItem,
                        ) {
                            Text(
                                if (section == MockRuleSection.FILE_NAME) {
                                    "이 항목 삭제"
                                } else {
                                    "이 단계 삭제"
                                }
                            )
                        }
                    }

                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::closeEditor,
                    ) {
                        Text("취소")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactRuleSummarySection(
    title: String,
    items: List<MockRuleItem?>,
    emptyLabel: (Int) -> String,
    onItemClick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = DDZColor.TextMuted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                val item = items.getOrNull(index)
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick(index) },
                ) {
                    Text(
                        text = item?.let(::ruleItemCompactLabel) ?: emptyLabel(index),
                        color = if (item == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun ruleItemCompactLabel(item: MockRuleItem): String =
    when (item.sourceType) {
        MockRuleSourceType.CELL -> "셀:${item.value}"
        MockRuleSourceType.MANUAL -> "직접:${item.value}"
        MockRuleSourceType.DATE -> "날짜"
        MockRuleSourceType.TIME -> "시간"
        MockRuleSourceType.ROTATING_TEXT -> "순환문구"
    }

package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.platform.AccessibilitySyncOptions
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateReadOnlyPreviewDialog
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListContent
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.theme.*
import platform.UIKit.UIViewController
import platform.Foundation.*

/** Simulator-only PoC host; the screen implementation is the existing shared dialog. */
@OptIn(ExperimentalComposeApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController(configure = {
    // Compose 1.7 requires explicit synchronization for XCTest without VoiceOver.
    // This host is PoC-only; Android and the shared dialog implementation are unchanged.
    accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
}) {
    DDZTheme {
        if (NSProcessInfo.processInfo.arguments.contains("--table-structure")) {
            TableStructurePoc()
        } else if (NSProcessInfo.processInfo.arguments.contains("--table-style")) {
            TableStylePoc()
        } else if (NSProcessInfo.processInfo.arguments.contains("--home-navigation")) {
            HomeNavigationPoc(remember { previewFixture() })
        } else {
        val template = remember {
            if (NSProcessInfo.processInfo.arguments.contains("--large-table")) scrollFixture() else previewFixture()
        }
        var open by remember { mutableStateOf(!NSProcessInfo.processInfo.arguments.contains("--template-navigation")) }
        var showList by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf(false) }
        if (showList) {
            TableTemplateListContent(
                templates = listOf(template), activeTemplateId = if (selected) template.id else null,
                onBack = { showList = false },
                onSelectTemplate = { selected = true; showList = false },
                // This memory-only PoC exposes selection/preview; persistence/editing remain later work.
                allowManagement = false,
                onOpenTemplate = {}, onCreateTemplate = {}, onRenameTemplate = { _, _ -> },
                onDuplicateTemplate = {}, onDeleteTemplate = {}, formatDate = ::formatPocTemplateDate,
            )
        } else Column(
            Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("DZlog", style = DDZTypography.HomeMainTitle, color = DDZColor.PrimaryDark)
            Text("템플릿 미리보기", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
            Text(if (selected) "선택한 템플릿 · ${template.name}" else "템플릿을 선택해 주세요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            Button(onClick = { open = true }) { Text("표 미리보기", style = DDZTypography.ButtonText) }
            DDZButton(text = "템플릿 목록", onClick = { showList = true })
        }
        if (open) TableTemplateReadOnlyPreviewDialog(
            template = template,
            onDismiss = { open = false },
            onSelect = { selected = true; open = false },
        )
        }
    }
}

/** Oversized synthetic table exercises both existing scroll containers through real gestures. */
private fun scrollFixture(): SavedTableTemplate = SavedTableTemplate(
    id = "poc-scroll", name = "스크롤 검증", modifiedAt = 0,
    templateState = TableTemplateState(16, 8, List(128) { index ->
        val row = index / 8
        val col = index % 8
        TableCellState(rowIndex = row, colIndex = col, cellId = "scroll-$index",
            dataType = TableCellDataType.TEXT,
            rawText = when {
                row == 0 && col == 0 -> "왼쪽 시작"
                row == 0 && col == 7 -> "오른쪽 끝"
                row == 15 && col == 0 -> "마지막 행"
                else -> "${row + 1}행 ${col + 1}열"
            })
    }), styleState = TableStyleState(),
)

/** Synthetic, memory-only sample. No photo, database, preference or active-template write. */
private fun previewFixture(): SavedTableTemplate {
    val types = listOf(TableCellDataType.TEXT, TableCellDataType.NUMBER, TableCellDataType.DATE,
        TableCellDataType.TIME, TableCellDataType.COUNTER, TableCellDataType.ROTATING_TEXT)
    val cells = buildList {
        for (index in 0 until 9) add(TableCellState(
            rowIndex = index / 3, colIndex = index % 3, cellId = "poc-$index",
            rowSpan = 1, colSpan = if (index == 0) 3 else 1,
            dataType = if (index < 3) TableCellDataType.TEXT else types[index - 3],
            rawText = when (index) { 0 -> "블루베리 관찰 기록"; 3 -> "Draper"; 4 -> "32"; else -> "" },
            phraseSetId = if (index == 8) "poc-phrases" else null,
        ))
    }
    return SavedTableTemplate(
        id = "poc-template", name = "현장 기록", modifiedAt = 0,
        templateState = TableTemplateState(3, 3, cells,
            rowWeights = listOf(0.8f, 1f, 1f), colWeights = listOf(1.2f, 1f, 1f),
            phraseSets = listOf(RotatingPhraseSet("poc-phrases", "처리", listOf("처리 A", "처리 B")))),
        styleState = TableStyleState(),
    )
}

/** Platform date presentation stays outside the shared screen, as on Android. */
private fun formatPocTemplateDate(timestamp: Long, used: Boolean): String {
    if (timestamp <= 0L) return if (used) "사용 기록 없음" else "수정일 없음"
    val formatter = NSDateFormatter().apply { dateFormat = "yyyy.MM.dd" }
    return (if (used) "사용 " else "수정 ") + formatter.stringFromDate(
        NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0))
}

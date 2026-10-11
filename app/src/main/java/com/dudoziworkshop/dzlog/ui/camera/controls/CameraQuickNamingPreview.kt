package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.ui.theme.*
import java.util.Date

/** Secondary output preview. Slot provenance, rather than string matching, controls emphasis. */
@Composable
internal fun CameraQuickNamingPreview(
    template: TableTemplateState,
    resolvedCells: List<ResolvedCell>,
    selectedId: String?,
    counterDigits: Int,
    counterSeed: Int?,
    now: Date,
    saveMode: SaveMode,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val previews = remember(template.fileNameSlotDrafts, template.pathSlotDrafts, resolvedCells, selectedId, counterDigits, counterSeed, now) {
        listOf(true, false).map { file ->
            buildNamingCellPreview(file, selectedId, resolvedCells,
                if (file) template.fileNameSlotDrafts else template.pathSlotDrafts,
                NamingFormatDefaults.FILE_NAME_DELIMITER, counterDigits, counterSeed, now,
                NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT)
        }
    }
    Surface(modifier, color = DDZColor.Background.copy(alpha = 0.9f),
        shape = RoundedCornerShape(DDZLayout.Radius.Small)) {
        Column(Modifier.padding(DDZSpacing.itemGap), verticalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
            val name = previews[0].text.replace(Regex("(?i)\\.jpe?g$"), "")
            PreviewRow(Icons.Filled.Description, "파일명", listOf(name), previews[0].highlights, compact)
            PreviewRow(Icons.Filled.Folder, "저장경로",
                capturePhysicalSavePaths(previews[1].text, saveMode), previews[1].highlights, compact)
        }
    }
}

@Composable
private fun PreviewRow(icon: ImageVector, label: String, texts: List<String>, ranges: List<IntRange>, compact: Boolean) {
    val text = buildAnnotatedString {
        texts.forEachIndexed { index, value ->
            if (index > 0) append("\n")
            val offset = length
            append(value)
            ranges.forEach { range ->
                val end = (range.last + 1).coerceAtMost(value.length)
                if (range.first < end) addStyle(SpanStyle(color = DDZColor.SageDarkStrong,
                    fontWeight = FontWeight.Medium), offset + range.first, offset + end)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
        Icon(icon, null, tint = DDZColor.IconMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
        Text(label, style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(text, style = DDZTypography.Caption, color = DDZColor.TextMuted,
            minLines = if (compact) 1 else 2, maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

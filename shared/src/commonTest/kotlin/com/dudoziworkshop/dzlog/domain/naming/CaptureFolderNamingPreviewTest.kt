package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureFolderNamingPreviewTest {
    private val base = "Pictures/DZlog/A/A/"
    // Only the second A comes from the selected cell.
    private val selected = base.lastIndexOf('A')..base.lastIndexOf('A')

    @Test
    fun bothFoldersRepeatOnlyTheSelectedContribution() {
        val original = base + "original/"
        val result = captureFolderNamingPreview(NamingCellPreview(base, listOf(selected)), SaveMode.BOTH)
        assertEquals("$base\n$original", result.text)
        val secondOffset = base.length + 1
        assertEquals(listOf(selected, (selected.first + secondOffset)..(selected.last + secondOffset)),
            result.highlights)
        assertEquals(listOf("A", "A"), result.highlights.map { result.text.substring(it) })
    }

    @Test
    fun originalAndWatermarkKeepOffsetsWithoutHighlightingTheSuffix() {
        for (mode in listOf(SaveMode.ORIGINAL_ONLY, SaveMode.WATERMARK_ONLY)) {
            val result = captureFolderNamingPreview(NamingCellPreview(base, listOf(selected)), mode)
            assertEquals(capturePhysicalSavePaths(base, mode).single(), result.text)
            assertEquals(listOf(selected), result.highlights)
        }
    }

    @Test
    fun emptyOrInvalidHighlightsDoNotChangeTheFolderText() {
        val plain = captureFolderNamingPreview(NamingCellPreview(base, emptyList()), SaveMode.BOTH)
        val invalid = captureFolderNamingPreview(NamingCellPreview(base,
            listOf(-1..0, base.length..base.length, 2..1)), SaveMode.BOTH)
        assertEquals(plain, invalid)
        assertEquals(emptyList(), invalid.highlights)
    }

    @Test
    fun alreadyOriginalAndRootPathsDoNotAddDuplicateOriginalFolders() {
        val original = base + "original/"
        assertEquals(NamingCellPreview(original, listOf(selected)),
            captureFolderNamingPreview(NamingCellPreview(original, listOf(selected)), SaveMode.ORIGINAL_ONLY))
        assertEquals(NamingCellPreview("Pictures/DZlog/\nPictures/DZlog/original/", emptyList()),
            captureFolderNamingPreview(NamingCellPreview("Pictures/DZlog/", emptyList()), SaveMode.BOTH))
    }
}

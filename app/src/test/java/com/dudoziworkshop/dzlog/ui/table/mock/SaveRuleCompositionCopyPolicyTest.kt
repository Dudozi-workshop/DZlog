package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveRuleCompositionCopyPolicyTest {
    private val fileRules = listOf(
        MockRuleItem(MockRuleSourceType.MANUAL, "Draper"),
        MockRuleItem(MockRuleSourceType.DATE, "20261017", formatPattern = "yyyyMMdd"),
        MockRuleItem(MockRuleSourceType.CELL, "sample", cellId = "cell-1"),
        null,
        null,
    )

    private val pathRules = listOf(
        MockRuleItem(MockRuleSourceType.MANUAL, "old-folder"),
        null,
        null,
        null,
        null,
    )

    private fun draft() = MockSaveRulesDraft(
        fileNameItems = fileRules,
        pathItems = pathRules,
        includePathInScope = true,
        includeFilenameInScope = false,
    )

    @Test
    fun `copy filename rules into path preserves formats and unrelated settings`() {
        val original = draft()
        val copied = copySaveRuleComposition(
            original,
            SaveRuleCopyDirection.FILE_NAME_TO_PATH,
            setOf("cell-1"),
        )
        assertEquals(fileRules, copied.pathItems)
        assertEquals(fileRules, copied.fileNameItems)
        assertTrue(copied.includePathInScope)
        assertFalse(copied.includeFilenameInScope)
        assertEquals(pathRules, original.pathItems)
    }

    @Test
    fun `copy folder rules into filename does not link future edits`() {
        val original = draft()
        val copied = copySaveRuleComposition(
            original,
            SaveRuleCopyDirection.PATH_TO_FILE_NAME,
            emptySet(),
        )
        assertEquals(pathRules, copied.fileNameItems)
        assertEquals(pathRules, copied.pathItems)
        assertEquals(fileRules, original.fileNameItems)

        val edited = copied.copy(fileNameItems = listOf(
            MockRuleItem(MockRuleSourceType.MANUAL, "new-name"),
            null, null, null, null,
        ))
        assertEquals(pathRules, edited.pathItems)
    }

    @Test
    fun `invalid or removed cell blocks entire copy without erasing destination`() {
        val original = draft()
        assertFalse(canCopySaveRuleComposition(
            original, SaveRuleCopyDirection.FILE_NAME_TO_PATH, emptySet()
        ))
        assertSame(original, copySaveRuleComposition(
            original, SaveRuleCopyDirection.FILE_NAME_TO_PATH, emptySet()
        ))
        assertEquals(pathRules, original.pathItems)
    }

    @Test
    fun `empty source and insufficient destination capacity never replace data`() {
        val original = draft()
        val empty = original.copy(fileNameItems = List(5) { null })
        assertSame(empty, copySaveRuleComposition(
            empty, SaveRuleCopyDirection.FILE_NAME_TO_PATH, emptySet()
        ))

        val tooSmall = original.copy(pathItems = listOf(null))
        assertSame(tooSmall, copySaveRuleComposition(
            tooSmall, SaveRuleCopyDirection.FILE_NAME_TO_PATH, setOf("cell-1")
        ))
    }
}

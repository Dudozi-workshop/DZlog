package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TableTemplateStateSlotFormatTest {

    @Test
    fun naming_slot_format_pattern_round_trips_through_json() {
        val dateSlot = TableEditorSlotDraft(
            kind = "FORMAT",
            label = "날짜",
            formatType = "DATE",
            formatPattern = "yyyy-MM-dd",
        )
        val initial = newBlankTableTemplateState(rows = 1, cols = 1).copy(
            fileNameSlotDrafts = listOf(dateSlot) +
                List((FILE_NAME_SLOT_COUNT - 1).coerceAtLeast(0)) { null },
        )

        val restored = tableTemplateStateFromJson(initial.toJsonString())

        assertNotNull(restored)
        assertEquals("DATE", restored!!.fileNameSlotDrafts.first()?.formatType)
        assertEquals("yyyy-MM-dd", restored.fileNameSlotDrafts.first()?.formatPattern)
    }
    @Test
    fun five_filename_and_path_slots_round_trip_without_data_loss() {
        val fileSlots = (1..FILE_NAME_SLOT_COUNT).map { number ->
            TableEditorSlotDraft(kind = "MANUAL", label = "filename$number", manualText = "F$number")
        }
        val pathSlots = (1..PATH_SLOT_UI_MAX_COUNT).map { number ->
            TableEditorSlotDraft(kind = "MANUAL", label = "folder$number", manualText = "P$number")
        }
        val initial = newBlankTableTemplateState(rows = 1, cols = 1).copy(
            fileNameSlotDrafts = fileSlots,
            pathSlotDrafts = pathSlots,
        )

        val restored = tableTemplateStateFromJson(initial.toJsonString())

        assertNotNull(restored)
        assertEquals(FILE_NAME_SLOT_COUNT, restored!!.fileNameSlotDrafts.size)
        assertEquals(PATH_SLOT_UI_MAX_COUNT, restored.pathSlotDrafts.size)
        assertEquals(fileSlots, restored.fileNameSlotDrafts)
        assertEquals(pathSlots, restored.pathSlotDrafts)
    }

    @Test
    fun legacy_three_slot_template_loads_with_two_empty_slots() {
        val initial = newBlankTableTemplateState(rows = 1, cols = 1).copy(
            fileNameSlotDrafts = (1..3).map { TableEditorSlotDraft(kind = "MANUAL", label = "F$it") },
            pathSlotDrafts = (1..3).map { TableEditorSlotDraft(kind = "MANUAL", label = "P$it") },
        )
        val restored = tableTemplateStateFromJson(initial.toJsonString())

        assertNotNull(restored)
        assertEquals(FILE_NAME_SLOT_COUNT, restored!!.fileNameSlotDrafts.size)
        assertEquals(PATH_SLOT_UI_MAX_COUNT, restored.pathSlotDrafts.size)
        assertEquals("F3", restored.fileNameSlotDrafts[2]?.label)
        assertEquals("P3", restored.pathSlotDrafts[2]?.label)
        assertNull(restored.fileNameSlotDrafts[3])
        assertNull(restored.fileNameSlotDrafts[4])
        assertNull(restored.pathSlotDrafts[3])
        assertNull(restored.pathSlotDrafts[4])
    }

}

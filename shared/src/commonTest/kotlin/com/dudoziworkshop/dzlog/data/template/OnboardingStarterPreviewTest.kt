package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OnboardingStarterPreviewTest {
    @Test fun everyStarterCanBuildTheHomePreviewBeforePersistence() {
        for (definition in OnboardingStarterCatalog.definitions) {
            val state = assertNotNull(OnboardingStarterCatalog.buildState(definition.id))
            assertEquals(FILE_NAME_SLOT_COUNT, state.fileNameSlotDrafts.size, definition.id)
            val used = (if (definition.filePrefix != null) 1 else 0) +
                definition.fileNameCells.size + (if (definition.includeFileDate) 1 else 0)
            assertEquals(used, state.fileNameSlotDrafts.filterNotNull().size, definition.id)
            assertEquals(List(FILE_NAME_SLOT_COUNT - used) { null }, state.fileNameSlotDrafts.drop(used))
            for (mode in SaveMode.entries) {
                buildPreview(PreviewInput(state, CaptureTime(0L, "en-US", "UTC"), 4,
                    "yyyyMMdd", "HHmm", "_", true, true, mode, null, 0))
            }
        }
    }
}

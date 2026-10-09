package com.dudoziworkshop.dzlog.ui.camera.effects

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.preview.TickUnit
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class CameraNowTickPolicyTest {
    @Test fun dateOnlyTableStillRefreshesIndependentTimeSlots() {
        val cells = listOf(TableCellState(0, 0, dataType = TableCellDataType.DATE))
        assertEquals(TickUnit.MINUTE, cameraNowTickUnit(cells))
    }

    @Test fun cameraRefreshesAtKoreanMidnightInsteadOfUtcMidnight() {
        val cells = listOf(TableCellState(0, 0, dataType = TableCellDataType.DATE))
        val now = ZonedDateTime.parse("2026-10-09T23:59:30+09:00[Asia/Seoul]")
        assertEquals(30_000L, computeNextDelayMillis(cameraNowTickUnit(cells), now.toInstant().toEpochMilli()))
    }

    @Test fun emptyTableStillRefreshesFormatSlots() {
        assertEquals(TickUnit.MINUTE, cameraNowTickUnit(emptyList()))
    }

    @Test fun secondsCompatibilityIsPreserved() {
        val cells = listOf(TableCellState(0, 0, dataType = TableCellDataType.TIME,
            timeFormatOptions = TimeFormatOptions(includeSeconds = true)))
        assertEquals(TickUnit.SECOND, cameraNowTickUnit(cells))
    }
}

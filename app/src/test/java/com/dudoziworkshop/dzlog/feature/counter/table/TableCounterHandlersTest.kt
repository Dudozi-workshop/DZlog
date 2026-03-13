package com.dudoziworkshop.dzlog.feature.counter.table

import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TableCounterHandlersTest {

    @Test
    fun `manual seed edit stays ui candidate when persist flag is false`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val facade = CounterFacade(context)
        val request = tableRequest()

        var templateState = counterTemplate(seed = 1)
        var counterUi = TableCounterUiState()

        updateCounterCellAndPolicy(
            templateState = templateState,
            cellId = templateState.cells.first().cellId,
            seed = 8,
            preserveManual = true,
            persistToCounterPolicy = false,
            counterRequest = request,
            counterFacade = facade,
            counterUi = counterUi,
            onTemplateChange = { templateState = it },
            setCounterUi = { counterUi = it },
            updateCell = ::updateCounterCell,
            scope = this,
        )

        val cellSeed = (templateState.cells.first().typedValue as CellValue.CounterSeed).start
        assertEquals(8, cellSeed)
        assertEquals(8, counterUi.scopeNextCounter)
        assertEquals(8, counterUi.manualSeedOverride)
        assertTrue(counterUi.isScopeCounterSynced)

        // 정책 핵심: 표 상세 수동 입력은 기본적으로 UI 후보값이며 domain manual override 저장이 아니다.
        val read = facade.read(request)
        assertEquals(1, read.next)
        assertFalse(read.hasManualOverride)
    }

    @Test
    fun `persist flag true writes manual override to policy`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val facade = CounterFacade(context)
        val request = tableRequest()

        var templateState = counterTemplate(seed = 1)
        var counterUi = TableCounterUiState()

        updateCounterCellAndPolicy(
            templateState = templateState,
            cellId = templateState.cells.first().cellId,
            seed = 7,
            preserveManual = true,
            persistToCounterPolicy = true,
            counterRequest = request,
            counterFacade = facade,
            counterUi = counterUi,
            onTemplateChange = { templateState = it },
            setCounterUi = { counterUi = it },
            updateCell = ::updateCounterCell,
            scope = this,
        )
        delay(20)

        val read = facade.read(request)
        assertEquals(7, read.next)
        assertTrue(read.hasManualOverride)
    }

    @Test
    fun `auto reset helper clears manual override then reads media based next`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val facade = CounterFacade(context)
        val request = tableRequest()

        facade.setManualNext(request, 5)
        val before = facade.read(request)
        assertTrue(before.hasManualOverride)

        val restored = fetchAutoNextCounter(
            counterRequest = request,
            counterFacade = facade,
        )

        assertEquals(1, restored)
        val after = facade.read(request)
        assertFalse(after.hasManualOverride)
    }

    @Test
    fun `conflict confirm applies global manual override for table scope`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val facade = CounterFacade(context)
        val request = tableRequest()

        var templateState = counterTemplate(seed = 1)
        var counterUi = TableCounterUiState()

        applyCounterConflictDialogEffect(
            effect = TableCounterConflictDialogEffect.ApplyManualSeed(
                cellId = templateState.cells.first().cellId,
                seed = 10,
            ),
            templateState = templateState,
            counterRequest = request,
            counterFacade = facade,
            counterUi = counterUi,
            onTemplateChange = { templateState = it },
            setCounterUi = { counterUi = it },
            updateCell = ::updateCounterCell,
            scope = this,
        )
        delay(20)

        val read = facade.read(request)
        assertEquals(10, read.next)
        assertTrue(read.hasManualOverride)
    }

    private fun tableRequest() = CounterRequestResolver.fromTable(
        saveMode = SaveMode.BOTH,
        relativePathKey = "Pictures/DZlog/table-handler/${UUID.randomUUID()}/",
        prefix = "table_handler_stream",
        scanPrefix = "table_handler_scan",
        includePathInScope = true,
        includeFilenameInScope = true,
        tableTemplateId = "table-handler-template",
    )

    private fun counterTemplate(seed: Int): TableTemplateState {
        val counterCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            dataType = TableCellDataType.COUNTER,
            typedValue = CellValue.CounterSeed(seed),
        )
        return TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(counterCell),
        )
    }

    private fun updateCounterCell(
        state: TableTemplateState,
        cellId: String,
        transform: (TableCellState) -> TableCellState,
    ): TableTemplateState {
        return state.copy(cells = state.cells.map { if (it.cellId == cellId) transform(it) else it })
    }
}

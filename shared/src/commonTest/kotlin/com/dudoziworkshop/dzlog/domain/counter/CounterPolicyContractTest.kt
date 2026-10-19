package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import kotlin.test.*

class CounterPolicyContractTest {
    @Test
    fun usedMediaNumbersUseMaxPlusOneWithoutReusingHoles() {
        assertEquals(1, CounterRules.nextFromUsed(emptySet()))
        assertEquals(4, CounterRules.nextFromUsed(setOf(1, 3)))
        assertEquals(3, CounterRules.nextFromUsed(setOf(1, 2)))
        assertEquals(13, CounterRules.nextFromUsed(setOf(2, 7, 12)))
    }

    @Test
    fun manualOverridePaddingAndZeroSeedKeepExistingTableBehavior() {
        val cell = TableCellState(0, 0, cellId = "n", dataType = TableCellDataType.COUNTER,
            rawText = "9999", typedValue = CellValue.CounterSeed(7))
        assertEquals(CounterNumberPlan(12, "0012", 13), planCounterNumber(cell, 12, 4))
        assertEquals(CounterNumberPlan(7, "0007", 8), planCounterNumber(cell, null, 4))
        assertEquals(CounterNumberPlan(0, "0000", 1), planCounterNumber(cell.copy(typedValue = CellValue.CounterSeed(0)), null, 4))
        assertEquals(CounterNumberPlan(1, "0001", 2), planCounterNumber(cell, 0, 4))
        assertEquals("12345", planCounterNumber(cell, 12345, 4).display)
        assertEquals("000012", planCounterNumber(cell, 12, 99).display)
        assertEquals("12", planCounterNumber(cell, 12, -1).display)
    }

    @Test
    fun timeScopeIsStillMinutePrecisionAndExcludesUnselectedDate() {
        val time = TableCellState(0, 0, cellId = "t", dataType = TableCellDataType.TIME,
            counterScopeMode = CounterScopeMode.INCLUDE)
        val date = TableCellState(0, 1, cellId = "d", dataType = TableCellDataType.DATE,
            counterScopeMode = CounterScopeMode.EXCLUDE)
        val result = CounterScopeResolver.resolve(CounterScopeResolver.Inputs(listOf(time, date), emptyList(),
            listOf(ResolvedCell("t", time.dataType, time, "13:14:59", false),
                ResolvedCell("d", date.dataType, date, "20261019", false))))
        assertEquals(listOf("1314"), result.timeScopeValues)
        assertEquals(emptyList(), result.dateScopeValues)
    }

    @Test
    fun explicitPhrasePolicyKeepsTheExistingEncodedStreamIdentityWhenFilenameScopeIsOff() {
        val options = CounterScopeOptions(rotatingPolicyScopeValues = listOf("continuous:실험 A"))
        assertEquals("name=off|rotating=continuous%3A%EC%8B%A4%ED%97%98+A", CounterRules.computeCounterPrefix(
            emptyList(), "_", emptyList(), false, options))
        assertEquals("%F0%9F%8C%B1+%2B%25", encodeFormTokenPortable("🌱 +%"))
    }
}

package com.dudoziworkshop.dzlog.domain.phrase

import org.junit.Assert.*
import org.junit.Test

class RotatingPhraseCsvCodecTest {
    private fun items(csv: String): List<String> =
        (RotatingPhraseCsvCodec.decode(csv) as RotatingPhraseCsvCodec.ImportResult.Success).items

    private fun invalid(csv: String) =
        assertTrue(RotatingPhraseCsvCodec.decode(csv) is RotatingPhraseCsvCodec.ImportResult.Failure)

    @Test fun utf8BomAndKoreanPhrasesRoundtripInOrder() {
        val original = listOf("초기 조사", "중간, 조사", "말기 \"최종\" 조사")
        val csv = RotatingPhraseCsvCodec.encode(original)
        assertTrue(csv.startsWith("\uFEFForder,phrase,every\r\n"))
        assertEquals(original, items(csv))
        assertTrue(csv.contains("\"중간, 조사\""))
        assertTrue(csv.contains("\"말기 \"\"최종\"\" 조사\""))
    }

    @Test fun acceptThreeColumnExcelCsvWithoutBomAndSortByOrder() {
        assertEquals(listOf("대조구", "처리구", "재관수"),
            items("order,phrase,every\r\n3,재관수,\r\n1,대조구,\r\n2,처리구,\r\n"))
    }

    @Test fun acceptQuotedCommasEscapedQuotesAndLineEndingVariants() {
        assertEquals(listOf("one,two", "three\"four"),
            items("order,phrase,every\n1,\"one,two\",\n2,\"three\"\"four\",\n"))
    }

    @Test fun invalidDataNeverProducesReplacementList() {
        invalid("")
        invalid("a,b\n1,고사")
        invalid("order,phrase,every\n")
        invalid("order,phrase,every\n1,,")
        invalid("order,phrase,every\n1,A,\n1,B,")
        invalid("order,phrase,every\n1,A,\n3,B,")
        invalid("order,phrase,every\nx,A,")
        invalid("order,phrase,every\n0,A,")
        invalid("order,phrase,every\n1,\"unterminated")
        invalid("order,phrase,every\n1,\"a\"junk,")
        invalid("order,phrase,every\n1,A,unexpected,extra")
        invalid("order,phrase,every\n1,\"line\nbreak\",")
    }

    @Test fun limitFileSizePhraseLengthAndRecordCount() {
        invalid("x".repeat(1_000_001))
        invalid("order,phrase,every\n1," + "a".repeat(501) + ",")
        val tooMany = buildString {
            append("order,phrase,every\n")
            repeat(1001) { append(it + 1).append(",A,\n") }
        }
        invalid(tooMany)
    }

    @Test fun newCsvExportsIndividualPeriodsAndInheritsBlankValues() {
        val values = listOf("초기", "중간", "처리 A", "처리 B")
        val intervals = listOf(null, null, 3, 4)
        val csv = RotatingPhraseCsvCodec.encode(values, intervals)
        assertTrue(csv.contains("3,\"처리 A\",3"))
        assertTrue(csv.contains("1,\"초기\","))
        val imported = RotatingPhraseCsvCodec.decode(csv)
            as RotatingPhraseCsvCodec.ImportResult.Success
        assertEquals(values, imported.items)
        assertEquals(intervals, imported.itemEveryOverrides)
    }

    @Test fun threeColumnCsvSortsPeriodsWithTheirPhraseRows() {
        val imported = RotatingPhraseCsvCodec.decode(
            "order,phrase,every\n3,C,3\n1,A,\n2,B,5\n"
        ) as RotatingPhraseCsvCodec.ImportResult.Success
        assertEquals(listOf("A", "B", "C"), imported.items)
        assertEquals(listOf(null, 5, 3), imported.itemEveryOverrides)
    }

    @Test fun legacyTwoColumnCsvIsRejectedWithoutChangingSavedSet() {
        val legacy = RotatingPhraseCsvCodec.decode("order,phrase\n2,B\n1,A\n")
        assertTrue(legacy is RotatingPhraseCsvCodec.ImportResult.Failure)
        assertTrue((legacy as RotatingPhraseCsvCodec.ImportResult.Failure).message.contains("3열"))
        invalid("order,phrase\n1,A")
        invalid("order,phrase,every\n1,A") // Missing every column is not an empty value.
    }

    @Test fun invalidPeriodsNeverSilentlyOverwriteSavedSet() {
        invalid("order,phrase,every\n1,A,0")
        invalid("order,phrase,every\n1,A,-1")
        invalid("order,phrase,every\n1,A,abc")
        invalid("order,phrase,every\n1,A,1.5")
        invalid("order,phrase,every\n1,A,2147483648")
        invalid("order,phrase,every\n1,A")
        invalid("order,phrase\n1,A,3")
    }

    @Test fun blankLinesDoNotMutateImportOrder() {
        assertEquals(listOf("A", "B"),
            items("\uFEFForder,phrase,every\r\n\r\n1,A,\r\n2,B,\r\n"))
    }
}

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
        assertTrue(csv.startsWith("\uFEFForder,phrase\r\n"))
        assertEquals(original, items(csv))
        assertTrue(csv.contains("\"중간, 조사\""))
        assertTrue(csv.contains("\"말기 \"\"최종\"\" 조사\""))
    }

    @Test fun acceptExcelCsvWithoutBomAndSortByOrder() {
        assertEquals(listOf("대조구", "처리구", "재관수"),
            items("order,phrase\r\n3,재관수\r\n1,대조구\r\n2,처리구\r\n"))
    }

    @Test fun acceptQuotedCommasEscapedQuotesAndLineEndingVariants() {
        assertEquals(listOf("one,two", "three\"four"),
            items("order,phrase\n1,\"one,two\"\n2,\"three\"\"four\"\n"))
    }

    @Test fun invalidDataNeverProducesReplacementList() {
        invalid("")
        invalid("a,b\n1,고사")
        invalid("order,phrase\n")
        invalid("order,phrase\n1,")
        invalid("order,phrase\n1,A\n1,B")
        invalid("order,phrase\n1,A\n3,B")
        invalid("order,phrase\nx,A")
        invalid("order,phrase\n0,A")
        invalid("order,phrase\n1,\"unterminated")
        invalid("order,phrase\n1,\"a\"junk")
        invalid("order,phrase\n1,A,unexpected")
        invalid("order,phrase\n1,\"line\nbreak\"")
    }

    @Test fun limitFileSizePhraseLengthAndRecordCount() {
        invalid("x".repeat(1_000_001))
        invalid("order,phrase\n1," + "a".repeat(501))
        val tooMany = buildString {
            append("order,phrase\n")
            repeat(1001) { append(it + 1).append(",A\n") }
        }
        invalid(tooMany)
    }

    @Test fun blankLinesDoNotMutateImportOrder() {
        assertEquals(listOf("A", "B"),
            items("\uFEFForder,phrase\r\n\r\n1,A\r\n2,B\r\n"))
    }
}

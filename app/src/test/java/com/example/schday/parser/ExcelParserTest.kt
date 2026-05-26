package com.example.schday.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class ExcelParserTest {

    // --- Helper to create InputStream from CSV string ---

    private fun csvStream(csv: String): InputStream {
        return ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
    }

    // --- Valid CSV parsing ---

    @Test
    fun parseCsv_singleRow_parsesCorrectly() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "高等数学,张教授,教三302,1,1,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16"
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(1, result.size)
        val (course, slots) = result[0]
        assertEquals("高等数学", course.name)
        assertEquals("张教授", course.teacher)
        assertEquals(1, course.semesterId)
        assertEquals("#D1E8E2", course.colorHex) // First Morandi color

        assertEquals(1, slots.size)
        val slot = slots[0]
        assertEquals(1, slot.dayOfWeek)
        assertEquals(1, slot.startPeriod)
        assertEquals(2, slot.endPeriod)
        assertEquals("教三302", slot.classroom)
        assertEquals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16", slot.activeWeeks)
    }

    @Test
    fun parseCsv_multipleRows_assignsDifferentColors() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "高等数学,张教授,A101,1,1,2,1-16\n" +
                "线性代数,李教授,B202,2,3,4,1-16"
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 2)

        assertEquals(2, result.size)
        assertEquals("#D1E8E2", result[0].first.colorHex) // MorandiColors[0]
        assertEquals("#E2D4F0", result[1].first.colorHex) // MorandiColors[1]
        assertEquals(2, result[0].first.semesterId)
        assertEquals(2, result[1].first.semesterId)
    }

    @Test
    fun parseCsv_weekRangeWithHyphen_preservedAsString() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "英语,王老师,C303,3,5,6,1-8"
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(1, result.size)
        assertEquals("1-8", result[0].second[0].activeWeeks)
    }

    @Test
    fun parseCsv_complexWeekList_preservedAsString() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "体育,赵老师,操场,5,7,8,1,3,5,7,9,11,13,15"
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals("1,3,5,7,9,11,13,15", result[0].second[0].activeWeeks)
    }

    // --- Empty file / header-only ---

    @Test
    fun parseCsv_headerOnly_returnsEmptyList() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数"
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun parseCsv_emptyStream_returnsEmptyList() {
        val result = ExcelParser.parseCsv(csvStream(""), semesterId = 1)
        assertTrue(result.isEmpty())
    }

    // --- Format error handling ---

    @Test
    fun parseCsv_rowWithTooFewColumns_isSkipped() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "高等数学,张教授,教三302\n" + // Only 3 columns, needs >= 7
                "英语,王老师,C303,3,5,6,1-8"  // Valid row
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(1, result.size)
        assertEquals("英语", result[0].first.name)
    }

    @Test
    fun parseCsv_invalidDayOfWeek_clampedToRange() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "课程A,老师,A,0,1,2,1-16\n" +   // dayOfWeek=0 -> clamped to 1
                "课程B,老师,B,8,1,2,1-16\n" +   // dayOfWeek=8 -> clamped to 7
                "课程C,老师,C,abc,1,2,1-16"      // dayOfWeek=abc -> toIntOrNull null -> default 1
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(3, result.size)
        assertEquals(1, result[0].second[0].dayOfWeek) // 0 clamped to 1
        assertEquals(7, result[1].second[0].dayOfWeek) // 8 clamped to 7
        assertEquals(1, result[2].second[0].dayOfWeek) // abc defaults to 1
    }

    @Test
    fun parseCsv_invalidPeriods_clampedToValidRange() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "课程A,老师,A,1,0,0,1-16\n" +    // startPeriod=0->1, endPeriod clamped to startPeriod(1)
                "课程B,老师,B,1,5,3,1-16\n" +     // endPeriod(3) < startPeriod(5) -> clamped to 5
                "课程C,老师,C,1,13,15,1-16"       // startPeriod=13 clamped to 12, endPeriod=15 clamped to 12
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(3, result.size)
        // Row 1: start clamped to 1, end clamped to 1 (can't be less than start)
        assertEquals(1, result[0].second[0].startPeriod)
        assertEquals(1, result[0].second[0].endPeriod)

        // Row 2: endPeriod(3) clamped up to startPeriod(5)
        assertEquals(5, result[1].second[0].startPeriod)
        assertEquals(5, result[1].second[0].endPeriod)

        // Row 3: both clamped to 12
        assertEquals(12, result[2].second[0].startPeriod)
        assertEquals(12, result[2].second[0].endPeriod)
    }

    @Test
    fun parseCsv_invalidPeriodsNonNumeric_defaultsApplied() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "课程A,老师,A,1,abc,xyz,1-16"  // startPeriod defaults to 1, endPeriod defaults to 2
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(1, result.size)
        assertEquals(1, result[0].second[0].startPeriod)
        assertEquals(2, result[0].second[0].endPeriod)
    }

    @Test
    fun parseCsv_quotesInWeeksStripped() {
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" +
                "课程A,老师,A,1,1,2,\"1-8\""
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(1, result.size)
        // Quotes should be stripped from the active weeks string
        assertEquals("1-8", result[0].second[0].activeWeeks)
    }

    @Test
    fun parseCsv_colorWrapsAround_afterExceedingMorandiColorsSize() {
        // MorandiColors has 8 entries
        val rows = (1..10).joinToString("\n") { idx ->
            "课程$idx,老师,A,$idx,1,2,1-16"
        }
        val csv = "课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数\n" + rows
        val result = ExcelParser.parseCsv(csvStream(csv), semesterId = 1)

        assertEquals(10, result.size)
        // First 8 should be MorandiColors[0..7], 9th wraps to [0], 10th to [1]
        assertEquals("#D1E8E2", result[0].first.colorHex)
        assertEquals("#EED1E6", result[7].first.colorHex) // MorandiColors[7]
        assertEquals("#D1E8E2", result[8].first.colorHex) // wraps to [0]
        assertEquals("#E2D4F0", result[9].first.colorHex) // wraps to [1]
    }
}

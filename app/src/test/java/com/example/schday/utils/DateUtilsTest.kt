package com.example.schday.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DateUtilsTest {

    @Test
    fun parseActiveWeeks_simpleList() {
        val input = "1, 2, 3, 4"
        val result = DateUtils.parseActiveWeeks(input)
        assertEquals(listOf(1, 2, 3, 4), result)
    }

    @Test
    fun parseActiveWeeks_withRanges() {
        val input = "1-4, 9-12"
        val result = DateUtils.parseActiveWeeks(input)
        assertEquals(listOf(1, 2, 3, 4, 9, 10, 11, 12), result)
    }

    @Test
    fun parseActiveWeeks_withTildeRanges() {
        val input = "5~8, 15"
        val result = DateUtils.parseActiveWeeks(input)
        assertEquals(listOf(5, 6, 7, 8, 15), result)
    }

    @Test
    fun parseActiveWeeks_mixedInvalid() {
        val input = "1, abc, 3-5, 8-xyz"
        val result = DateUtils.parseActiveWeeks(input)
        assertEquals(listOf(1, 3, 4, 5), result)
    }

    @Test
    fun isWeekActive_checksCorrectly() {
        val input = "1-8, 10"
        assertTrue(DateUtils.isWeekActive(input, 5))
        assertTrue(DateUtils.isWeekActive(input, 10))
        assertFalse(DateUtils.isWeekActive(input, 9))
        assertFalse(DateUtils.isWeekActive(input, 11))
    }

    // --- parseActiveWeeks boundary cases ---

    @Test
    fun parseActiveWeeks_singleValue() {
        val result = DateUtils.parseActiveWeeks("5")
        assertEquals(listOf(5), result)
    }

    @Test
    fun parseActiveWeeks_emptyString_returnsEmptyList() {
        val result = DateUtils.parseActiveWeeks("")
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun parseActiveWeeks_reversedRange_stillProducesAscendingList() {
        // Range "8-3" should produce [3,4,5,6,7,8] (the code handles start > end)
        val result = DateUtils.parseActiveWeeks("8-3")
        assertEquals(listOf(3, 4, 5, 6, 7, 8), result)
    }

    @Test
    fun parseActiveWeeks_reversedTildeRange_stillProducesAscendingList() {
        val result = DateUtils.parseActiveWeeks("10~5")
        assertEquals(listOf(5, 6, 7, 8, 9, 10), result)
    }

    @Test
    fun parseActiveWeeks_allInvalid_returnsEmptyList() {
        val result = DateUtils.parseActiveWeeks("abc, def, xyz")
        assertEquals(emptyList<Int>(), result)
    }

    @Test
    fun parseActiveWeeks_mixedListAndRanges() {
        val result = DateUtils.parseActiveWeeks("1, 3-5, 8, 10-12")
        assertEquals(listOf(1, 3, 4, 5, 8, 10, 11, 12), result)
    }

    @Test
    fun parseActiveWeeks_duplicateValues_allowed() {
        val result = DateUtils.parseActiveWeeks("1, 1, 2-3, 3")
        assertEquals(listOf(1, 1, 2, 3, 3), result)
    }

    @Test
    fun parseActiveWeeks_singleValueRange() {
        val result = DateUtils.parseActiveWeeks("5-5")
        assertEquals(listOf(5), result)
    }

    // --- isWeekActive boundary cases ---

    @Test
    fun isWeekActive_emptyString_returnsFalse() {
        assertFalse(DateUtils.isWeekActive("", 1))
    }

    @Test
    fun isWeekActive_weekZero_returnsFalse() {
        assertFalse(DateUtils.isWeekActive("1-16", 0))
    }

    // --- formatPeriodTime ---

    @Test
    fun formatPeriodTime_combinesStartAndEnd() {
        assertEquals("08:00-08:45", DateUtils.formatPeriodTime("08:00", "08:45"))
    }

    @Test
    fun formatPeriodTime_emptyStrings() {
        assertEquals("-", DateUtils.formatPeriodTime("", ""))
    }

    // --- getCurrentWeek boundary cases ---

    @Test
    fun getCurrentWeek_startDateInFuture_returns1() {
        // Use a date far in the future
        val futureStart = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000 // 1 year from now
        val result = DateUtils.getCurrentWeek(futureStart, 20)
        assertEquals(1, result)
    }

    @Test
    fun getCurrentWeek_withinFirstWeek_returns1() {
        // Start date is 3 days ago (less than 1 full week)
        val startDate = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
        val result = DateUtils.getCurrentWeek(startDate, 20)
        assertEquals(1, result)
    }

    @Test
    fun getCurrentWeek_inSecondWeek_returns2() {
        // Start date is 8 days ago (> 1 week, < 2 weeks)
        val startDate = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        val result = DateUtils.getCurrentWeek(startDate, 20)
        assertEquals(2, result)
    }

    @Test
    fun getCurrentWeek_exceedsTotalWeeks_clampedToTotal() {
        // Start date is 200 days ago, with only 16 weeks total
        val startDate = System.currentTimeMillis() - 200L * 24 * 60 * 60 * 1000
        val result = DateUtils.getCurrentWeek(startDate, 16)
        assertEquals(16, result)
    }

    @Test
    fun getCurrentWeek_exactlyAtTotalWeeksBoundary() {
        // Start date is exactly 19 weeks ago (week 20)
        val startDate = System.currentTimeMillis() - 19L * 7 * 24 * 60 * 60 * 1000
        val result = DateUtils.getCurrentWeek(startDate, 20)
        assertEquals(20, result)
    }
}

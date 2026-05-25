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
}

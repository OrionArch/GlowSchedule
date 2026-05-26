package com.example.schday.utils

import com.example.schday.data.entity.ScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleClashDetectorTest {

    @Test
    fun findClashes_noClash_differentDays() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-10")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 2, startPeriod = 1, endPeriod = 2, classroom = "A102", activeWeeks = "1-10")
        
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_noClash_nonOverlappingPeriods() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-10")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 3, endPeriod = 4, classroom = "A102", activeWeeks = "1-10")
        
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_noClash_nonOverlappingWeeks() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-8")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A102", activeWeeks = "9-16")
        
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_hasClash_exactOverlap() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-10")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A102", activeWeeks = "5-15")
        
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertEquals(1, clashes.size)
        assertEquals(newSlot, clashes[0].first)
        assertEquals(existingSlot, clashes[0].second)
    }

    @Test
    fun findClashes_hasClash_partialPeriodOverlap() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 3, startPeriod = 2, endPeriod = 4, classroom = "A101", activeWeeks = "1-10")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 3, startPeriod = 4, endPeriod = 5, classroom = "A102", activeWeeks = "8")
        
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertEquals(1, clashes.size)
        assertEquals(newSlot, clashes[0].first)
        assertEquals(existingSlot, clashes[0].second)
    }
}

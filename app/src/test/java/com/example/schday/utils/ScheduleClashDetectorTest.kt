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

    @Test
    fun findClashes_noClash_adjacentPeriodsNoOverlap() {
        // newSlot ends at period 2, existingSlot starts at period 3 -- no overlap
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-10")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 3, endPeriod = 4, classroom = "A102", activeWeeks = "1-10")

        // startPeriod(3) > endPeriod(2) -> overlapPeriod = false
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_hasClash_singleWeekOverlap() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "5")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A102", activeWeeks = "3-7")

        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertEquals(1, clashes.size)
    }

    @Test
    fun findClashes_hasClash_containedPeriod() {
        // newSlot fully contains existingSlot
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 2, startPeriod = 1, endPeriod = 6, classroom = "A101", activeWeeks = "1-16")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 2, startPeriod = 3, endPeriod = 4, classroom = "A102", activeWeeks = "1-16")

        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertEquals(1, clashes.size)
    }

    @Test
    fun findClashes_multipleNewSlots_eachCheckedAgainstAllExisting() {
        val newSlot1 = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A", activeWeeks = "1-10")
        val newSlot2 = ScheduleSlot(id = 2, courseId = 1, dayOfWeek = 2, startPeriod = 3, endPeriod = 4, classroom = "B", activeWeeks = "1-10")

        val existingSlot1 = ScheduleSlot(id = 3, courseId = 2, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "C", activeWeeks = "1-10")
        val existingSlot2 = ScheduleSlot(id = 4, courseId = 2, dayOfWeek = 2, startPeriod = 3, endPeriod = 4, classroom = "D", activeWeeks = "1-10")

        val clashes = ScheduleClashDetector.findClashes(
            listOf(newSlot1, newSlot2),
            listOf(existingSlot1, existingSlot2)
        )
        assertEquals(2, clashes.size)
    }

    @Test
    fun findClashes_emptyNewSlots_returnsEmpty() {
        val existingSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A", activeWeeks = "1-10")
        val clashes = ScheduleClashDetector.findClashes(emptyList(), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_emptyExistingSlots_returnsEmpty() {
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A", activeWeeks = "1-10")
        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), emptyList())
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_bothEmpty_returnsEmpty() {
        val clashes = ScheduleClashDetector.findClashes(emptyList(), emptyList())
        assertTrue(clashes.isEmpty())
    }

    @Test
    fun findClashes_noClash_sameDayAndPeriodButDifferentWeeks() {
        // Same day, same periods, but weeks do not intersect
        val newSlot = ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A", activeWeeks = "1-8")
        val existingSlot = ScheduleSlot(id = 2, courseId = 2, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "B", activeWeeks = "9-16")

        val clashes = ScheduleClashDetector.findClashes(listOf(newSlot), listOf(existingSlot))
        assertTrue(clashes.isEmpty())
    }
}

package com.example.schday.utils

import com.example.schday.data.entity.ScheduleSlot

object ScheduleClashDetector {
    /**
     * Finds clashes between a list of new slots and a list of existing slots.
     * Returns a list of Pairs of (new conflicting slot, existing conflicting slot).
     */
    fun findClashes(
        newSlots: List<ScheduleSlot>,
        existingSlots: List<ScheduleSlot>
    ): List<Pair<ScheduleSlot, ScheduleSlot>> {
        val clashes = mutableListOf<Pair<ScheduleSlot, ScheduleSlot>>()
        for (newSlot in newSlots) {
            val newWeeks = DateUtils.parseActiveWeeks(newSlot.activeWeeks).toSet()
            for (existingSlot in existingSlots) {
                if (newSlot.dayOfWeek == existingSlot.dayOfWeek) {
                    val overlapPeriod = newSlot.startPeriod <= existingSlot.endPeriod &&
                            existingSlot.startPeriod <= newSlot.endPeriod
                    if (overlapPeriod) {
                        val existingWeeks = DateUtils.parseActiveWeeks(existingSlot.activeWeeks).toSet()
                        val commonWeeks = newWeeks.intersect(existingWeeks)
                        if (commonWeeks.isNotEmpty()) {
                            clashes.add(Pair(newSlot, existingSlot))
                        }
                    }
                }
            }
        }
        return clashes
    }
}

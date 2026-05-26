package com.example.schday.utils

import android.content.Context
import java.util.*

object DateUtils {

    fun getCurrentWeek(startDateMillis: Long, totalWeeks: Int): Int {
        val now = System.currentTimeMillis()
        if (now < startDateMillis) return 1
        val diffMillis = now - startDateMillis
        val diffWeeks = (diffMillis / (7 * 24 * 60 * 60 * 1000)).toInt()
        val current = diffWeeks + 1
        return when {
            current > totalWeeks -> totalWeeks
            current < 1 -> 1
            else -> current
        }
    }

    fun getDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    fun parseActiveWeeks(activeWeeksStr: String): List<Int> {
        return activeWeeksStr.split(",").flatMap { part ->
            val trimmed = part.trim()
            val rangeParts = when {
                trimmed.contains("-") -> trimmed.split("-")
                trimmed.contains("~") -> trimmed.split("~")
                else -> null
            }
            if (rangeParts != null && rangeParts.size == 2) {
                val start = rangeParts[0].trim().toIntOrNull()
                val end = rangeParts[1].trim().toIntOrNull()
                if (start != null && end != null) {
                    if (start <= end) (start..end).toList() else (end..start).toList()
                } else {
                    emptyList()
                }
            } else {
                listOfNotNull(trimmed.toIntOrNull())
            }
        }
    }

    fun isWeekActive(activeWeeksStr: String, week: Int): Boolean {
        return parseActiveWeeks(activeWeeksStr).contains(week)
    }

    fun formatPeriodTime(startTime: String, endTime: String): String {
        return "$startTime-$endTime"
    }

    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }

    fun getDayName(context: Context, dayOfWeek: Int): String {
        val names = context.resources.getStringArray(com.example.schday.R.array.day_of_week_names)
        val index = dayOfWeek - 1
        return if (index in names.indices) names[index] else ""
    }

    fun triggerHapticFeedback(context: android.content.Context, milliseconds: Long = 30) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(milliseconds, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

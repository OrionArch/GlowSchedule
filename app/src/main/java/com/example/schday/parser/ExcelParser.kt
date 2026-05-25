package com.example.schday.parser

import com.example.schday.data.entity.Course
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.theme.MorandiColors
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object ExcelParser {

    /**
     * Parses a CSV containing course schedules:
     * Header: 课程名称,任课教师,上课教室,星期几,开始节次,结束节次,上课周数
     * Example: 高等数学,张教授,教三302,1,1,2,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16
     */
    fun parseCsv(inputStream: InputStream, semesterId: Int): List<Pair<Course, List<ScheduleSlot>>> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val result = mutableListOf<Pair<Course, List<ScheduleSlot>>>()
        var line: String? = reader.readLine() // Skip header

        var colorIdx = 0

        while (reader.readLine().also { line = it } != null) {
            val parts = line!!.split(",").map { it.trim() }
            if (parts.size < 7) continue
            val name = parts[0]
            val teacher = parts[1]
            val classroom = parts[2]
            val dayOfWeek = parts[3].toIntOrNull() ?: 1
            val startPeriod = parts[4].toIntOrNull() ?: 1
            val endPeriod = parts[5].toIntOrNull() ?: 2
            
            // Join remaining fields to rebuild week numbers list if split on commas
            val activeWeeks = parts.subList(6, parts.size).joinToString(",").replace("\"", "")

            val course = Course(
                semesterId = semesterId,
                name = name,
                teacher = teacher,
                colorHex = MorandiColors[colorIdx % MorandiColors.size]
            )
            colorIdx++

            val slot = ScheduleSlot(
                courseId = 0,
                dayOfWeek = dayOfWeek,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                classroom = classroom,
                activeWeeks = activeWeeks
            )

            result.add(Pair(course, listOf(slot)))
        }
        return result
    }
}

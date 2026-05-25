package com.example.schday.parser

import android.util.Base64
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.ScheduleSlot
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object GlowCodeManager {

    data class SharedSlot(
        val dayOfWeek: Int,
        val startPeriod: Int,
        val endPeriod: Int,
        val classroom: String,
        val activeWeeks: String
    )

    data class SharedCourse(
        val name: String,
        val teacher: String,
        val colorHex: String,
        val slots: List<SharedSlot>
    )

    data class GlowCodeData(
        val title: String,
        val courses: List<SharedCourse>
    )

    /**
     * Encodes a list of courses into a Base64 string prefixed with glow://
     */
    fun encode(title: String, courses: List<CourseWithSchedules>): String {
        try {
            val rootObj = JSONObject()
            rootObj.put("title", title)

            val coursesArr = JSONArray()
            for (cWithS in courses) {
                val courseObj = JSONObject()
                courseObj.put("name", cWithS.course.name)
                courseObj.put("teacher", cWithS.course.teacher)
                courseObj.put("colorHex", cWithS.course.colorHex)

                val slotsArr = JSONArray()
                for (slot in cWithS.slots) {
                    val slotObj = JSONObject()
                    slotObj.put("dayOfWeek", slot.dayOfWeek)
                    slotObj.put("startPeriod", slot.startPeriod)
                    slotObj.put("endPeriod", slot.endPeriod)
                    slotObj.put("classroom", slot.classroom)
                    slotObj.put("activeWeeks", slot.activeWeeks)
                    slotsArr.put(slotObj)
                }
                courseObj.put("slots", slotsArr)
                coursesArr.put(courseObj)
            }
            rootObj.put("courses", coursesArr)

            val bytes = rootObj.toString().toByteArray(StandardCharsets.UTF_8)
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            return "glow://$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Decodes a glow:// Base64 string into GlowCodeData
     */
    fun decode(glowCode: String): GlowCodeData? {
        if (!glowCode.startsWith("glow://")) return null
        try {
            val base64 = glowCode.substring(7).trim()
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val jsonStr = String(bytes, StandardCharsets.UTF_8)
            val rootObj = JSONObject(jsonStr)

            val title = rootObj.optString("title", "已共享的课表")
            val coursesArr = rootObj.getJSONArray("courses")
            val coursesList = mutableListOf<SharedCourse>()

            for (i in 0 until coursesArr.length()) {
                val courseObj = coursesArr.getJSONObject(i)
                val name = courseObj.getString("name")
                val teacher = courseObj.optString("teacher", "")
                val colorHex = courseObj.optString("colorHex", "#98A78F")

                val slotsArr = courseObj.getJSONArray("slots")
                val slotsList = mutableListOf<SharedSlot>()
                for (j in 0 until slotsArr.length()) {
                    val slotObj = slotsArr.getJSONObject(j)
                    slotsList.add(
                        SharedSlot(
                            dayOfWeek = slotObj.getInt("dayOfWeek"),
                            startPeriod = slotObj.getInt("startPeriod"),
                            endPeriod = slotObj.getInt("endPeriod"),
                            classroom = slotObj.optString("classroom", ""),
                            activeWeeks = slotObj.getString("activeWeeks")
                        )
                    )
                }
                coursesList.add(SharedCourse(name, teacher, colorHex, slotsList))
            }
            return GlowCodeData(title, coursesList)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

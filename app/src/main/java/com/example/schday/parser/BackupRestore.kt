package com.example.schday.parser

import com.example.schday.data.DataRepository
import com.example.schday.data.entity.*
import com.example.schday.theme.MorandiColors
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object BackupRestore {

    suspend fun exportToJson(repository: DataRepository): String {
        val root = JSONObject()
        root.put("version", 1)

        val semestersArray = JSONArray()
        val semesters = repository.getAllSemesters().first()
        for (sem in semesters) {
            val semObj = JSONObject()
            semObj.put("id", sem.id)
            semObj.put("name", sem.name)
            semObj.put("startDate", sem.startDate)
            semObj.put("totalWeeks", sem.totalWeeks)
            semObj.put("isCurrent", sem.isCurrent)
            semestersArray.put(semObj)
        }
        root.put("semesters", semestersArray)

        val coursesArray = JSONArray()
        for (sem in semesters) {
            val courses = repository.getCoursesBySemester(sem.id).first()
            for (cWithS in courses) {
                val cObj = JSONObject()
                cObj.put("semesterId", cWithS.course.semesterId)
                cObj.put("name", cWithS.course.name)
                cObj.put("teacher", cWithS.course.teacher)
                cObj.put("colorHex", cWithS.course.colorHex)

                val slotsArray = JSONArray()
                for (slot in cWithS.slots) {
                    val slotObj = JSONObject()
                    slotObj.put("dayOfWeek", slot.dayOfWeek)
                    slotObj.put("startPeriod", slot.startPeriod)
                    slotObj.put("endPeriod", slot.endPeriod)
                    slotObj.put("classroom", slot.classroom)
                    slotObj.put("activeWeeks", slot.activeWeeks)
                    slotsArray.put(slotObj)
                }
                cObj.put("slots", slotsArray)

                val homeworkArray = JSONArray()
                for (hw in cWithS.homework) {
                    val hwObj = JSONObject()
                    hwObj.put("title", hw.title)
                    hwObj.put("description", hw.description)
                    hwObj.put("deadline", hw.deadline)
                    hwObj.put("isCompleted", hw.isCompleted)
                    homeworkArray.put(hwObj)
                }
                cObj.put("homework", homeworkArray)

                coursesArray.put(cObj)
            }
        }
        root.put("courses", coursesArray)

        val periodsArray = JSONArray()
        val periods = repository.getAllPeriodTimes().first()
        for (p in periods) {
            val pObj = JSONObject()
            pObj.put("periodNumber", p.periodNumber)
            pObj.put("startTime", p.startTime)
            pObj.put("endTime", p.endTime)
            periodsArray.put(pObj)
        }
        root.put("periods", periodsArray)

        return root.toString(2)
    }

    suspend fun importFromJson(repository: DataRepository, jsonStr: String) {
        val root = JSONObject(jsonStr)

        val semestersArray = root.optJSONArray("semesters")
        val semesterIdMap = mutableMapOf<Int, Int>()
        if (semestersArray != null) {
            for (i in 0 until semestersArray.length()) {
                val semObj = semestersArray.getJSONObject(i)
                val oldId = semObj.getInt("id")
                val sem = Semester(
                    name = semObj.getString("name"),
                    startDate = semObj.getLong("startDate"),
                    totalWeeks = semObj.getInt("totalWeeks"),
                    isCurrent = semObj.getBoolean("isCurrent")
                )
                val newId = repository.insertSemester(sem).toInt()
                semesterIdMap[oldId] = newId
            }
        }

        val periodsArray = root.optJSONArray("periods")
        if (periodsArray != null) {
            val list = mutableListOf<PeriodTime>()
            for (i in 0 until periodsArray.length()) {
                val pObj = periodsArray.getJSONObject(i)
                list.add(
                    PeriodTime(
                        periodNumber = pObj.getInt("periodNumber"),
                        startTime = pObj.getString("startTime"),
                        endTime = pObj.getString("endTime")
                    )
                )
            }
            if (list.isNotEmpty()) {
                repository.insertPeriodTimes(list)
            }
        }

        val coursesArray = root.optJSONArray("courses")
        if (coursesArray != null) {
            for (i in 0 until coursesArray.length()) {
                val cObj = coursesArray.getJSONObject(i)
                val oldSemId = cObj.getInt("semesterId")
                val newSemId = semesterIdMap[oldSemId] ?: continue

                val course = Course(
                    semesterId = newSemId,
                    name = cObj.getString("name"),
                    teacher = cObj.optString("teacher", ""),
                    colorHex = cObj.optString("colorHex", MorandiColors.first())
                )

                val slotsArray = cObj.optJSONArray("slots")
                val slots = mutableListOf<ScheduleSlot>()
                if (slotsArray != null) {
                    for (j in 0 until slotsArray.length()) {
                        val slotObj = slotsArray.getJSONObject(j)
                        slots.add(
                            ScheduleSlot(
                                courseId = 0,
                                dayOfWeek = slotObj.getInt("dayOfWeek"),
                                startPeriod = slotObj.getInt("startPeriod"),
                                endPeriod = slotObj.getInt("endPeriod"),
                                classroom = slotObj.optString("classroom", ""),
                                activeWeeks = slotObj.getString("activeWeeks")
                            )
                        )
                    }
                }

                repository.saveCourseWithSlots(course, slots)

                val insertedCourseWithSchedules = repository.getCoursesBySemester(newSemId).first()
                    .find { it.course.name == course.name && it.course.semesterId == newSemId }
                val newCourseId = insertedCourseWithSchedules?.course?.id ?: continue

                val homeworkArray = cObj.optJSONArray("homework")
                if (homeworkArray != null) {
                    for (j in 0 until homeworkArray.length()) {
                        val hwObj = homeworkArray.getJSONObject(j)
                        val hw = Homework(
                            courseId = newCourseId,
                            title = hwObj.getString("title"),
                            description = hwObj.optString("description", ""),
                            deadline = hwObj.getLong("deadline"),
                            isCompleted = hwObj.getBoolean("isCompleted")
                        )
                        repository.insertHomework(hw)
                    }
                }
            }
        }
    }
}

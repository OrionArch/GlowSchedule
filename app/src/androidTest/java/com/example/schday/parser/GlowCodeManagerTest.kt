package com.example.schday.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.ScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlowCodeManagerTest {

    // --- Helpers ---

    private fun makeCourseWithSchedules(
        courseId: Int = 1,
        name: String = "高等数学",
        teacher: String = "张教授",
        colorHex: String = "#D1E8E2",
        slots: List<ScheduleSlot> = listOf(
            ScheduleSlot(id = 1, courseId = courseId, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-16")
        )
    ): CourseWithSchedules {
        return CourseWithSchedules(
            course = Course(id = courseId, semesterId = 1, name = name, teacher = teacher, colorHex = colorHex),
            slots = slots,
            homework = emptyList()
        )
    }

    // --- Encode / Decode roundtrip ---

    @Test
    fun encodeDecode_roundtrip_singleCourse() {
        val original = makeCourseWithSchedules()
        val glowCode = GlowCodeManager.encode("测试课表", listOf(original))

        assertTrue(glowCode.startsWith("glow://"))
        val decoded = GlowCodeManager.decode(glowCode)

        assertNotNull(decoded)
        assertEquals("测试课表", decoded!!.title)
        assertEquals(1, decoded.courses.size)

        val course = decoded.courses[0]
        assertEquals("高等数学", course.name)
        assertEquals("张教授", course.teacher)
        assertEquals("#D1E8E2", course.colorHex)
        assertEquals(1, course.slots.size)

        val slot = course.slots[0]
        assertEquals(1, slot.dayOfWeek)
        assertEquals(1, slot.startPeriod)
        assertEquals(2, slot.endPeriod)
        assertEquals("A101", slot.classroom)
        assertEquals("1-16", slot.activeWeeks)
    }

    @Test
    fun encodeDecode_roundtrip_multipleCoursesMultipleSlots() {
        val courses = listOf(
            makeCourseWithSchedules(
                courseId = 1, name = "高等数学", teacher = "张教授",
                slots = listOf(
                    ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "A101", activeWeeks = "1-16"),
                    ScheduleSlot(id = 2, courseId = 1, dayOfWeek = 3, startPeriod = 3, endPeriod = 4, classroom = "B202", activeWeeks = "1-8")
                )
            ),
            makeCourseWithSchedules(
                courseId = 2, name = "线性代数", teacher = "李教授", colorHex = "#E2D4F0",
                slots = listOf(
                    ScheduleSlot(id = 3, courseId = 2, dayOfWeek = 2, startPeriod = 5, endPeriod = 6, classroom = "C303", activeWeeks = "1,3,5,7,9")
                )
            )
        )

        val glowCode = GlowCodeManager.encode("多课程测试", courses)
        val decoded = GlowCodeManager.decode(glowCode)

        assertNotNull(decoded)
        assertEquals("多课程测试", decoded!!.title)
        assertEquals(2, decoded.courses.size)

        // First course: 2 slots
        assertEquals("高等数学", decoded.courses[0].name)
        assertEquals(2, decoded.courses[0].slots.size)
        assertEquals("B202", decoded.courses[0].slots[1].classroom)

        // Second course: 1 slot
        assertEquals("线性代数", decoded.courses[1].name)
        assertEquals(1, decoded.courses[1].slots.size)
        assertEquals("1,3,5,7,9", decoded.courses[1].slots[0].activeWeeks)
    }

    @Test
    fun encodeDecode_roundtrip_unicodeContent() {
        val courses = listOf(
            makeCourseWithSchedules(name = "日语（二）", teacher = "田中先生", colorHex = "#F7D6D0",
                slots = listOf(
                    ScheduleSlot(id = 1, courseId = 1, dayOfWeek = 4, startPeriod = 7, endPeriod = 8, classroom = "外语楼305", activeWeeks = "1~16")
                )
            )
        )

        val glowCode = GlowCodeManager.encode("Unicodeテスト", courses)
        val decoded = GlowCodeManager.decode(glowCode)

        assertNotNull(decoded)
        assertEquals("Unicodeテスト", decoded!!.title)
        assertEquals("日语（二）", decoded.courses[0].name)
        assertEquals("田中先生", decoded.courses[0].teacher)
        assertEquals("外语楼305", decoded.courses[0].slots[0].classroom)
        assertEquals("1~16", decoded.courses[0].slots[0].activeWeeks)
    }

    // --- Decode boundary cases ---

    @Test
    fun decode_invalidPrefix_returnsNull() {
        assertNull(GlowCodeManager.decode("invalid"))
        assertNull(GlowCodeManager.decode("http://something"))
        assertNull(GlowCodeManager.decode(""))
    }

    @Test
    fun decode_validPrefixButInvalidBase64_returnsNull() {
        assertNull(GlowCodeManager.decode("glow://!!!not-base64!!!"))
    }

    @Test
    fun decode_validBase64ButInvalidJson_returnsNull() {
        // Valid Base64 but not valid JSON with expected structure
        val invalidJson = "not json at all"
        val base64 = android.util.Base64.encodeToString(
            invalidJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        assertNull(GlowCodeManager.decode("glow://$base64"))
    }

    @Test
    fun decode_emptyCoursesArray_returnsEmptyCourseList() {
        val emptyJson = """{"title":"空课表","courses":[]}"""
        val base64 = android.util.Base64.encodeToString(
            emptyJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val decoded = GlowCodeManager.decode("glow://$base64")

        assertNotNull(decoded)
        assertEquals("空课表", decoded!!.title)
        assertTrue(decoded.courses.isEmpty())
    }

    @Test
    fun decode_missingTitleUsesDefault() {
        val json = """{"courses":[{"name":"数学","teacher":"","colorHex":"#D1E8E2","slots":[]}]}"""
        val base64 = android.util.Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val decoded = GlowCodeManager.decode("glow://$base64")

        assertNotNull(decoded)
        assertEquals("已共享的课表", decoded!!.title) // Default title
    }

    @Test
    fun decode_missingOptionalSlotFields_usesDefaults() {
        val json = """{"title":"test","courses":[{"name":"数学","slots":[{"dayOfWeek":1,"startPeriod":1,"endPeriod":2,"activeWeeks":"1-8"}]}]}"""
        val base64 = android.util.Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
        )
        val decoded = GlowCodeManager.decode("glow://$base64")

        assertNotNull(decoded)
        val slot = decoded!!.courses[0].slots[0]
        assertEquals("", slot.classroom) // Default empty classroom
        assertEquals("1-8", slot.activeWeeks)
    }

    @Test
    fun encode_emptyCourseList_producesValidGlowCode() {
        val glowCode = GlowCodeManager.encode("空", emptyList())
        assertTrue(glowCode.startsWith("glow://"))

        val decoded = GlowCodeManager.decode(glowCode)
        assertNotNull(decoded)
        assertEquals("空", decoded!!.title)
        assertTrue(decoded.courses.isEmpty())
    }
}

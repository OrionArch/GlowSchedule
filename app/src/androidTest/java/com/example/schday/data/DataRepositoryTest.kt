package com.example.schday.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.schday.data.entity.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultDataRepository(db, context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- Semester ----

    @Test
    fun insertAndRetrieveSemester() = runTest {
        val semester = Semester(
            name = "2025 Fall",
            startDate = 1725148800000L, // 2024-09-02
            totalWeeks = 18,
            isCurrent = false
        )
        val id = repository.insertSemester(semester)
        assertThat(id).isGreaterThan(0)

        val allSemesters = repository.getAllSemesters().first()
        assertThat(allSemesters).hasSize(1)
        assertThat(allSemesters[0].name).isEqualTo("2025 Fall")
        assertThat(allSemesters[0].totalWeeks).isEqualTo(18)
    }

    @Test
    fun setCurrentSemester() = runTest {
        val s1 = repository.insertSemester(
            Semester(name = "Spring 2025", startDate = 1700000000000L)
        )
        val s2 = repository.insertSemester(
            Semester(name = "Fall 2025", startDate = 1725148800000L)
        )

        // Set s2 as current
        repository.setCurrentSemester(s2.toInt())

        val current = repository.getCurrentSemester().first()
        assertThat(current).isNotNull()
        assertThat(current!!.name).isEqualTo("Fall 2025")
        assertThat(current.isCurrent).isTrue()

        // Now switch to s1
        repository.setCurrentSemester(s1.toInt())
        val currentNow = repository.getCurrentSemester().first()
        assertThat(currentNow!!.name).isEqualTo("Spring 2025")
    }

    // ---- Course & ScheduleSlots ----

    @Test
    fun insertAndRetrieveCourseWithScheduleSlots() = runTest {
        val semesterId = repository.insertSemester(
            Semester(name = "Fall 2025", startDate = 1725148800000L, isCurrent = true)
        ).toInt()

        val course = Course(
            semesterId = semesterId,
            name = "Linear Algebra",
            teacher = "Prof. Wang",
            colorHex = "#FF5722"
        )
        val slots = listOf(
            ScheduleSlot(
                courseId = 0,
                dayOfWeek = 1,
                startPeriod = 1,
                endPeriod = 2,
                classroom = "Room 301",
                activeWeeks = "1,2,3,4,5,6,7,8,9,10"
            ),
            ScheduleSlot(
                courseId = 0,
                dayOfWeek = 3,
                startPeriod = 3,
                endPeriod = 4,
                classroom = "Room 302",
                activeWeeks = "1,2,3,4,5,6,7,8,9,10"
            )
        )

        val courseId = repository.saveCourseWithSlots(course, slots)
        assertThat(courseId).isGreaterThan(0)

        val coursesWithSchedules = repository.getCoursesBySemester(semesterId).first()
        assertThat(coursesWithSchedules).hasSize(1)

        val cws = coursesWithSchedules[0]
        assertThat(cws.course.name).isEqualTo("Linear Algebra")
        assertThat(cws.course.teacher).isEqualTo("Prof. Wang")
        assertThat(cws.slots).hasSize(2)

        // Verify slots have correct courseId assigned
        assertThat(cws.slots.all { it.courseId == courseId.toInt() }).isTrue()

        // Verify slot content
        val mondaySlot = cws.slots.find { it.dayOfWeek == 1 }
        assertThat(mondaySlot).isNotNull()
        assertThat(mondaySlot!!.classroom).isEqualTo("Room 301")
        assertThat(mondaySlot.startPeriod).isEqualTo(1)
        assertThat(mondaySlot.endPeriod).isEqualTo(2)
    }

    @Test
    fun deleteCourse() = runTest {
        val semesterId = repository.insertSemester(
            Semester(name = "Fall 2025", startDate = 1725148800000L)
        ).toInt()

        val courseId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "ToDelete"),
            listOf(
                ScheduleSlot(
                    courseId = 0, dayOfWeek = 2, startPeriod = 1,
                    endPeriod = 2, classroom = "A", activeWeeks = "1"
                )
            )
        )

        var courses = repository.getCoursesBySemester(semesterId).first()
        assertThat(courses).hasSize(1)

        repository.deleteCourse(courses[0].course)

        courses = repository.getCoursesBySemester(semesterId).first()
        assertThat(courses).isEmpty()
    }

    // ---- Homework ----

    @Test
    fun insertAndRetrieveHomework() = runTest {
        val semesterId = repository.insertSemester(
            Semester(name = "Fall 2025", startDate = 1725148800000L)
        ).toInt()

        val courseId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "Physics"),
            emptyList()
        ).toInt()

        val hw = Homework(
            courseId = courseId,
            title = "Chapter 3 Exercises",
            description = "Problems 3.1 to 3.15",
            deadline = 1730000000000L,
            isCompleted = false
        )
        val hwId = repository.insertHomework(hw)
        assertThat(hwId).isGreaterThan(0)

        val allHw = repository.getAllHomework().first()
        assertThat(allHw).hasSize(1)
        assertThat(allHw[0].title).isEqualTo("Chapter 3 Exercises")
        assertThat(allHw[0].description).isEqualTo("Problems 3.1 to 3.15")
        assertThat(allHw[0].isCompleted).isFalse()

        // Verify filtering by course
        val courseHw = repository.getHomeworkForCourse(courseId).first()
        assertThat(courseHw).hasSize(1)

        // Verify uncompleted filter
        val uncompleted = repository.getUncompletedHomework().first()
        assertThat(uncompleted).hasSize(1)
    }

    @Test
    fun deleteHomework() = runTest {
        val semesterId = repository.insertSemester(
            Semester(name = "Fall 2025", startDate = 1725148800000L)
        ).toInt()

        val courseId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "Chemistry"),
            emptyList()
        ).toInt()

        repository.insertHomework(
            Homework(
                courseId = courseId,
                title = "Lab Report",
                deadline = 1730000000000L
            )
        )

        var allHw = repository.getAllHomework().first()
        assertThat(allHw).hasSize(1)

        repository.deleteHomework(allHw[0])

        allHw = repository.getAllHomework().first()
        assertThat(allHw).isEmpty()
    }

    // ---- PeriodTimes ----

    @Test
    fun insertPeriodTimes() = runTest {
        val periods = listOf(
            PeriodTime(periodNumber = 1, startTime = "08:00", endTime = "08:45"),
            PeriodTime(periodNumber = 2, startTime = "08:55", endTime = "09:40"),
            PeriodTime(periodNumber = 3, startTime = "10:00", endTime = "10:45"),
            PeriodTime(periodNumber = 4, startTime = "10:55", endTime = "11:40")
        )

        repository.insertPeriodTimes(periods)

        val stored = repository.getAllPeriodTimes().first()
        assertThat(stored).hasSize(4)

        // Verify ordered by periodNumber
        assertThat(stored[0].periodNumber).isEqualTo(1)
        assertThat(stored[0].startTime).isEqualTo("08:00")
        assertThat(stored[0].endTime).isEqualTo("08:45")

        assertThat(stored[3].periodNumber).isEqualTo(4)
        assertThat(stored[3].startTime).isEqualTo("10:55")
    }

    // ---- Semester deletion cascade ----

    @Test
    fun deleteSemesterCascadesToCourses() = runTest {
        val semesterId = repository.insertSemester(
            Semester(name = "ToDelete", startDate = 1725148800000L)
        ).toInt()

        repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "Orphan"),
            listOf(
                ScheduleSlot(
                    courseId = 0, dayOfWeek = 1, startPeriod = 1,
                    endPeriod = 2, classroom = "X", activeWeeks = "1"
                )
            )
        )

        assertThat(repository.getCoursesBySemester(semesterId).first()).hasSize(1)

        repository.deleteSemester(Semester(id = semesterId, name = "ToDelete", startDate = 1725148800000L))

        assertThat(repository.getCoursesBySemester(semesterId).first()).isEmpty()
    }
}

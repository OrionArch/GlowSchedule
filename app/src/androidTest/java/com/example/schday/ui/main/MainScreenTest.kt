package com.example.schday.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    val repository = FakeDataRepositoryForTest()
    composeTestRule.setContent { 
      MainScreen(repository = repository, onItemClick = {}) 
    }
  }

  @Test
  fun bottomNavigation_isDisplayed() {
    composeTestRule.onNodeWithText("今日").assertExists()
    composeTestRule.onNodeWithText("课表").assertExists()
    composeTestRule.onNodeWithText("待办").assertExists()
    composeTestRule.onNodeWithText("设置").assertExists()
  }
}

private class FakeDataRepositoryForTest : DataRepository {
    private val semestersFlow = MutableStateFlow<List<Semester>>(emptyList())
    private val currentSemesterFlow = MutableStateFlow<Semester?>(null)
    private val coursesFlow = MutableStateFlow<List<CourseWithSchedules>>(emptyList())
    private val homeworkFlow = MutableStateFlow<List<Homework>>(emptyList())
    private val periodTimesFlow = MutableStateFlow<List<PeriodTime>>(emptyList())

    override fun getAllSemesters(): Flow<List<Semester>> = semestersFlow
    override fun getCurrentSemester(): Flow<Semester?> = currentSemesterFlow
    override suspend fun insertSemester(semester: Semester): Long = 0
    override suspend fun updateSemester(semester: Semester) {}
    override suspend fun deleteSemester(semester: Semester) {}
    override suspend fun setCurrentSemester(semesterId: Int) {}

    override fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>> = coursesFlow
    override fun getCourseById(courseId: Int): Flow<CourseWithSchedules?> = flow { emit(null) }
    override suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>) {}
    override suspend fun deleteCourse(course: Course) {}

    override fun getAllHomework(): Flow<List<Homework>> = homeworkFlow
    override fun getUncompletedHomework(): Flow<List<Homework>> = homeworkFlow
    override fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>> = homeworkFlow
    override suspend fun insertHomework(homework: Homework): Long = 0
    override suspend fun updateHomework(homework: Homework) {}
    override suspend fun deleteHomework(homework: Homework) {}

    override fun getAllPeriodTimes(): Flow<List<PeriodTime>> = periodTimesFlow
    override suspend fun insertPeriodTimes(periods: List<PeriodTime>) {}
}

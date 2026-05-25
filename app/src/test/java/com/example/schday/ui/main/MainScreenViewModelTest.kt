package com.example.schday.ui.main

import com.example.schday.data.DataRepository
import com.example.schday.data.entity.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectTab_updatesCurrentTab() = runTest {
        val repository = FakeDataRepository()
        val viewModel = MainScreenViewModel(repository)
        assertEquals(0, viewModel.currentTab.value)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.currentTab.value)
    }

    @Test
    fun selectWeek_updatesSelectedWeek() = runTest {
        val repository = FakeDataRepository()
        val viewModel = MainScreenViewModel(repository)
        assertEquals(null, viewModel.selectedWeek.value)
        viewModel.selectWeek(5)
        assertEquals(5, viewModel.selectedWeek.value)
    }

    @Test
    fun insertHomework_updatesHomeworkList() = runTest {
        val repository = FakeDataRepository()
        val viewModel = MainScreenViewModel(repository)
        val originalSize = repository.getAllHomework().first().size
        val homework = Homework(id = 99, courseId = 1, title = "Math PS1", description = "Problems 1-10", deadline = 1772841600000L, isCompleted = false)
        
        viewModel.insertHomework(homework)
        
        val homeworkList = repository.getAllHomework().first()
        assertEquals(originalSize + 1, homeworkList.size)
        assertEquals("Math PS1", homeworkList.last().title)
    }
}

private class FakeDataRepository : DataRepository {
    private val semestersFlow = MutableStateFlow<List<Semester>>(emptyList())
    private val currentSemesterFlow = MutableStateFlow<Semester?>(null)
    private val coursesFlow = MutableStateFlow<List<CourseWithSchedules>>(emptyList())
    private val homeworkFlow = MutableStateFlow<List<Homework>>(emptyList())
    private val periodTimesFlow = MutableStateFlow<List<PeriodTime>>(emptyList())

    override fun getAllSemesters(): Flow<List<Semester>> = semestersFlow
    override fun getCurrentSemester(): Flow<Semester?> = currentSemesterFlow
    
    override suspend fun insertSemester(semester: Semester): Long {
        val list = semestersFlow.value.toMutableList()
        val newSem = semester.copy(id = list.size + 1)
        list.add(newSem)
        semestersFlow.value = list
        if (newSem.isCurrent) {
            currentSemesterFlow.value = newSem
        }
        return newSem.id.toLong()
    }

    override suspend fun updateSemester(semester: Semester) {
        val list = semestersFlow.value.map { if (it.id == semester.id) semester else it }
        semestersFlow.value = list
    }

    override suspend fun deleteSemester(semester: Semester) {
        semestersFlow.value = semestersFlow.value.filter { it.id != semester.id }
    }

    override suspend fun setCurrentSemester(semesterId: Int) {
        val list = semestersFlow.value.map { it.copy(isCurrent = it.id == semesterId) }
        semestersFlow.value = list
        currentSemesterFlow.value = list.find { it.isCurrent }
    }

    override fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>> = coursesFlow
    override fun getCourseById(courseId: Int): Flow<CourseWithSchedules?> = flow {
        emit(coursesFlow.value.find { it.course.id == courseId })
    }

    override suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>): Long {
        val list = coursesFlow.value.toMutableList()
        list.removeAll { it.course.id == course.id }
        val id = if (course.id == 0) list.size + 1 else course.id
        val newCourse = course.copy(id = id)
        list.add(CourseWithSchedules(newCourse, slots.map { it.copy(courseId = id) }, emptyList()))
        coursesFlow.value = list
        return id.toLong()
    }

    override suspend fun deleteCourse(course: Course) {
        coursesFlow.value = coursesFlow.value.filter { it.course.id != course.id }
    }

    override fun getAllHomework(): Flow<List<Homework>> = homeworkFlow
    override fun getUncompletedHomework(): Flow<List<Homework>> = flow {
        emit(homeworkFlow.value.filter { !it.isCompleted })
    }

    override fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>> = flow {
        emit(homeworkFlow.value.filter { it.courseId == courseId })
    }

    override suspend fun insertHomework(homework: Homework): Long {
        val list = homeworkFlow.value.toMutableList()
        val newHw = homework.copy(id = list.size + 1)
        list.add(newHw)
        homeworkFlow.value = list
        return newHw.id.toLong()
    }

    override suspend fun updateHomework(homework: Homework) {
        homeworkFlow.value = homeworkFlow.value.map { if (it.id == homework.id) homework else it }
    }

    override suspend fun deleteHomework(homework: Homework) {
        homeworkFlow.value = homeworkFlow.value.filter { it.id != homework.id }
    }

    override fun getAllPeriodTimes(): Flow<List<PeriodTime>> = periodTimesFlow
    override suspend fun insertPeriodTimes(periods: List<PeriodTime>) {
        periodTimesFlow.value = periods
    }
}

package com.example.schday.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    private val _selectedWeek = MutableStateFlow<Int?>(null)
    val selectedWeek: StateFlow<Int?> = _selectedWeek.asStateFlow()

    fun selectWeek(week: Int) {
        _selectedWeek.value = week
    }

    val semesters = repository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSemester = repository.getCurrentSemester()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodTimes = repository.getAllPeriodTimes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHomework = repository.getAllHomework()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val courses = currentSemester.flatMapLatest { semester ->
        if (semester == null) flowOf(emptyList())
        else repository.getCoursesBySemester(semester.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllPeriodTimes().first().let { list ->
                if (list.isEmpty()) {
                    val defaultPeriods = listOf(
                        PeriodTime(1, "08:00", "08:45"),
                        PeriodTime(2, "08:50", "09:35"),
                        PeriodTime(3, "09:50", "10:35"),
                        PeriodTime(4, "10:40", "11:25"),
                        PeriodTime(5, "11:30", "12:15"),
                        PeriodTime(6, "13:30", "14:15"),
                        PeriodTime(7, "14:20", "15:05"),
                        PeriodTime(8, "15:20", "16:05"),
                        PeriodTime(9, "16:10", "16:55"),
                        PeriodTime(10, "17:00", "17:45"),
                        PeriodTime(11, "18:30", "19:15"),
                        PeriodTime(12, "19:20", "20:05")
                    )
                    repository.insertPeriodTimes(defaultPeriods)
                }
            }

            repository.getAllSemesters().first().let { list ->
                if (list.isEmpty()) {
                    val calendar = Calendar.getInstance()
                    calendar.firstDayOfWeek = Calendar.MONDAY
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val semesterId = repository.insertSemester(
                        Semester(
                            name = "2026 春季学期",
                            startDate = calendar.timeInMillis,
                            totalWeeks = 20,
                            isCurrent = true
                        )
                    ).toInt()

                    val math = Course(semesterId = semesterId, name = "人工智能导论", teacher = "林教授", colorHex = "#C7E2F7")
                    repository.saveCourseWithSlots(
                        math,
                        listOf(
                            ScheduleSlot(courseId = 0, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "数智楼 404", activeWeeks = "1-20"),
                            ScheduleSlot(courseId = 0, dayOfWeek = 3, startPeriod = 3, endPeriod = 4, classroom = "数智楼 404", activeWeeks = "1-20")
                        )
                    )

                    val english = Course(semesterId = semesterId, name = "算法与数据结构", teacher = "李教授", colorHex = "#E2D4F0")
                    repository.saveCourseWithSlots(
                        english,
                        listOf(
                            ScheduleSlot(courseId = 0, dayOfWeek = 2, startPeriod = 3, endPeriod = 4, classroom = "信工楼 201", activeWeeks = "1-20")
                        )
                    )
                }
            }
        }
    }

    fun insertHomework(homework: Homework) {
        viewModelScope.launch { repository.insertHomework(homework) }
    }

    fun updateHomework(homework: Homework) {
        viewModelScope.launch { repository.updateHomework(homework) }
    }

    fun deleteHomework(homework: Homework) {
        viewModelScope.launch { repository.deleteHomework(homework) }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { repository.deleteCourse(course) }
    }

    fun insertSemester(semester: Semester) {
        viewModelScope.launch { repository.insertSemester(semester) }
    }

    fun updateSemester(semester: Semester) {
        viewModelScope.launch { repository.updateSemester(semester) }
    }

    fun selectSemester(semesterId: Int) {
        viewModelScope.launch { repository.setCurrentSemester(semesterId) }
    }
}

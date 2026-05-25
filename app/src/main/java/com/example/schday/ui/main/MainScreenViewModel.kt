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
                    loadDemoData()
                }
            }
        }
    }

    private suspend fun loadDemoData() {
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

        // 1. 人工智能导论
        val aiId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "人工智能导论", teacher = "林教授", colorHex = "#D6E4FF"),
            listOf(
                ScheduleSlot(courseId = 0, dayOfWeek = 1, startPeriod = 1, endPeriod = 2, classroom = "数智楼 404", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20"),
                ScheduleSlot(courseId = 0, dayOfWeek = 3, startPeriod = 3, endPeriod = 4, classroom = "数智楼 404", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
            )
        ).toInt()

        // 2. 算法与数据结构
        val algoId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "算法与数据结构", teacher = "李教授", colorHex = "#F3E0EC"),
            listOf(
                ScheduleSlot(courseId = 0, dayOfWeek = 2, startPeriod = 3, endPeriod = 4, classroom = "信工楼 201", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
            )
        ).toInt()

        // 3. 编译原理
        val compilerId = repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "编译原理", teacher = "王教授", colorHex = "#FFD3D3"),
            listOf(
                ScheduleSlot(courseId = 0, dayOfWeek = 4, startPeriod = 1, endPeriod = 2, classroom = "理学楼 102", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16"),
                ScheduleSlot(courseId = 0, dayOfWeek = 5, startPeriod = 5, endPeriod = 6, classroom = "理学楼 102", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16")
            )
        ).toInt()

        // 4. 计算机网络
        repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "计算机网络", teacher = "张教授", colorHex = "#D2F4EA"),
            listOf(
                ScheduleSlot(courseId = 0, dayOfWeek = 1, startPeriod = 5, endPeriod = 6, classroom = "实验楼 Rm 504", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16")
            )
        )

        // 5. 大学物理
        repository.saveCourseWithSlots(
            Course(semesterId = semesterId, name = "大学物理", teacher = "陈教授", colorHex = "#FFE8D6"),
            listOf(
                ScheduleSlot(courseId = 0, dayOfWeek = 3, startPeriod = 1, endPeriod = 2, classroom = "教一 101", activeWeeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16")
            )
        )

        // 3 Example homework tasks
        val now = System.currentTimeMillis()
        repository.insertHomework(
            Homework(
                courseId = compilerId,
                title = "书面作业：词法分析器构建练习",
                description = "练习手写 DFA 和 NFA 转换",
                deadline = now + 24 * 60 * 60 * 1000 // 1 day later
            )
        )
        repository.insertHomework(
            Homework(
                courseId = algoId,
                title = "大作业：红黑树的插入与删除实现",
                description = "实现完整的树旋转和自平衡着色逻辑",
                deadline = now + 48 * 60 * 60 * 1000 // 2 days later
            )
        )
        repository.insertHomework(
            Homework(
                courseId = aiId,
                title = "实验：利用 PyTorch 构建简单神经网络",
                description = "实现 MNIST 手写数字分类任务",
                deadline = now + 5 * 24 * 60 * 60 * 1000 // 5 days later
            )
        )
    }

    fun importGlowCode(data: com.example.schday.parser.GlowCodeManager.GlowCodeData, semesterId: Int) {
        viewModelScope.launch {
            data.courses.forEach { sharedCourse ->
                val course = Course(
                    semesterId = semesterId,
                    name = sharedCourse.name,
                    teacher = sharedCourse.teacher,
                    colorHex = sharedCourse.colorHex
                )
                val slots = sharedCourse.slots.map { s ->
                    ScheduleSlot(
                        courseId = 0,
                        dayOfWeek = s.dayOfWeek,
                        startPeriod = s.startPeriod,
                        endPeriod = s.endPeriod,
                        classroom = s.classroom,
                        activeWeeks = s.activeWeeks
                    )
                }
                repository.saveCourseWithSlots(course, slots)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Delete all semesters which cascades to courses, slots, homework
            repository.getAllSemesters().first().forEach { sem ->
                repository.deleteSemester(sem)
            }
        }
    }

    fun loadDemoDataManually() {
        viewModelScope.launch {
            loadDemoData()
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

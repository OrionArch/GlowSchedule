package com.example.schday.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.schday.AddEditCourse
import com.example.schday.ImportCourses
import com.example.schday.data.DataRepository
import com.example.schday.parser.BackupRestore
import com.example.schday.ui.screens.home.HomeTab
import com.example.schday.ui.screens.settings.SettingsTab
import com.example.schday.ui.screens.timetable.TimetableTab
import com.example.schday.ui.screens.todo.TodoTab
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    repository: DataRepository,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val currentSemester by viewModel.currentSemester.collectAsStateWithLifecycle()
    val periods by viewModel.periodTimes.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val homeworkList by viewModel.allHomework.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()

    // Reactively reschedule alarms when courses change
    LaunchedEffect(courses, periods) {
        if (courses.isNotEmpty() && periods.isNotEmpty()) {
            com.example.schday.scheduler.AlarmScheduler.scheduleTodayAlarms(context, repository)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "今日") },
                    label = { Text("今日") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "课表") },
                    label = { Text("课表") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.List, contentDescription = "待办") },
                    label = { Text("待办") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    HomeTab(
                        semester = currentSemester,
                        courses = courses,
                        periods = periods,
                        onAddCourseClick = { onItemClick(AddEditCourse(null)) }
                    )
                }
                1 -> {
                    TimetableTab(
                        semester = currentSemester,
                        courses = courses,
                        periods = periods,
                        selectedWeek = selectedWeek,
                        onWeekSelected = { viewModel.selectWeek(it) },
                        onCourseClick = { courseId -> onItemClick(AddEditCourse(courseId)) }
                    )
                }
                2 -> {
                    TodoTab(
                        courses = courses,
                        homeworkList = homeworkList,
                        onAddHomework = { viewModel.insertHomework(it) },
                        onUpdateHomework = { viewModel.updateHomework(it) },
                        onDeleteHomework = { viewModel.deleteHomework(it) }
                    )
                }
                3 -> {
                    SettingsTab(
                        semesters = semesters,
                        currentSemester = currentSemester,
                        periods = periods,
                        onSelectSemester = { viewModel.selectSemester(it) },
                        onAddSemester = { viewModel.insertSemester(it) },
                        onUpdatePeriods = { list ->
                            coroutineScope.launch {
                                repository.insertPeriodTimes(list)
                            }
                        },
                        onImportJson = { jsonStr ->
                            coroutineScope.launch {
                                BackupRestore.importFromJson(repository, jsonStr)
                            }
                        },
                        onExportJson = {
                            var json = ""
                            // Synchronously running export by blocking briefly or launching in a runBlocking manner isn't recommended, 
                            // but since exportToJson is suspendable, we can obtain it safely by launching or using a state.
                            // To keep it clean, we block with runBlocking or use a callback. Let's run blocking since it's local db.
                            kotlinx.coroutines.runBlocking {
                                json = BackupRestore.exportToJson(repository)
                            }
                            json
                        },
                        onImportClick = { onItemClick(ImportCourses) }
                    )
                }
            }
        }
    }
}

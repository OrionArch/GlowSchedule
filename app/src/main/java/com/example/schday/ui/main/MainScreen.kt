package com.example.schday.ui.main

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.schday.AddEditCourse
import com.example.schday.ImportCourses
import com.example.schday.data.DataRepository
import com.example.schday.parser.BackupRestore
import com.example.schday.parser.GlowCodeManager
import com.example.schday.theme.hudBackground
import com.example.schday.ui.screens.home.HomeTab
import com.example.schday.ui.screens.settings.SettingsTab
import com.example.schday.ui.screens.timetable.TimetableTab
import com.example.schday.ui.screens.todo.TodoTab
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    repository: DataRepository,
    onItemClick: (NavKey) -> Unit,
    appTheme: com.example.schday.theme.GlowTheme,
    onThemeChange: (com.example.schday.theme.GlowTheme) -> Unit,
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

    // Clipboard Glow Code scanning logic
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingGlowCode by remember { mutableStateOf<String?>(null) }
    var processedGlowCode by remember { mutableStateOf("") }
    var glowCodeData by remember { mutableStateOf<GlowCodeManager.GlowCodeData?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                try {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                        if (text.startsWith("glow://") && text != processedGlowCode) {
                            val decoded = GlowCodeManager.decode(text)
                            if (decoded != null) {
                                glowCodeData = decoded
                                pendingGlowCode = text
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reactively reschedule alarms when courses change
    LaunchedEffect(courses, periods) {
        if (courses.isNotEmpty() && periods.isNotEmpty()) {
            com.example.schday.scheduler.AlarmScheduler.scheduleTodayAlarms(context, repository)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().hudBackground(appTheme),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 8.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple(0, Icons.Default.Home, "今日"),
                        Triple(1, Icons.Default.DateRange, "课表"),
                        Triple(2, Icons.Default.List, "待办"),
                        Triple(3, Icons.Default.Settings, "设置")
                    )
                    tabs.forEach { (index, icon, label) ->
                        val selected = currentTab == index
                        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            Color.Transparent
                        }
                        val contentColor = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { viewModel.selectTab(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(containerColor)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
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
                        appTheme = appTheme,
                        onAddCourseClick = { onItemClick(AddEditCourse(null)) }
                    )
                }
                1 -> {
                    TimetableTab(
                        semester = currentSemester,
                        courses = courses,
                        periods = periods,
                        appTheme = appTheme,
                        selectedWeek = selectedWeek,
                        onWeekSelected = { viewModel.selectWeek(it) },
                        onCourseClick = { courseId -> onItemClick(AddEditCourse(courseId)) }
                    )
                }
                2 -> {
                    TodoTab(
                        courses = courses,
                        homeworkList = homeworkList,
                        appTheme = appTheme,
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
                        appTheme = appTheme,
                        onThemeChange = onThemeChange,
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
                        onImportClick = { onItemClick(ImportCourses) },
                        onClearData = { viewModel.clearAllData() },
                        onLoadDemoData = { viewModel.loadDemoDataManually() }
                    )
                }
            }
        }
    }

    if (glowCodeData != null) {
        val data = glowCodeData!!
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            processedGlowCode = pendingGlowCode ?: ""
            glowCodeData = null
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐈 发现拾光分享口令",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "检测到剪贴板中含有班级共享课表：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "「${data.title}」",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(data.courses) { course ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (course.teacher.isNotEmpty()) "(${course.teacher})" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                processedGlowCode = pendingGlowCode ?: ""
                                glowCodeData = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("忽略")
                        }

                        Button(
                            onClick = {
                                val semId = currentSemester?.id
                                if (semId != null) {
                                    viewModel.importGlowCode(data, semId)
                                    Toast.makeText(context, "成功导入 ${data.courses.size} 门课程！", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "请先去设置页面创建一个学期！", Toast.LENGTH_LONG).show()
                                }
                                processedGlowCode = pendingGlowCode ?: ""
                                glowCodeData = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("导入课表")
                        }
                    }
                }
            }
        }
    }
}

package com.example.schday.ui.screens.todo

import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.Homework
import com.example.schday.theme.getContrastingTextColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTab(
    courses: List<CourseWithSchedules>,
    homeworkList: List<Homework>,
    onAddHomework: (Homework) -> Unit,
    onUpdateHomework: (Homework) -> Unit,
    onDeleteHomework: (Homework) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCourseIndex by remember { mutableStateOf<Int?>(null) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    // Date picker state
    val calendar = Calendar.getInstance()
    var selectedDeadlineMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val uncompletedTasks = remember(homeworkList) { homeworkList.filter { !it.isCompleted } }
    val completedTasks = remember(homeworkList) { homeworkList.filter { it.isCompleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "作业与考试待办",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Quick Add Task Form
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("快速添加待办", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Course Selector dropdown
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedButton(
                            onClick = { courseDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val cName = selectedCourseIndex?.let { courses.getOrNull(it)?.course?.name } ?: "关联课程"
                            Text(cName, fontSize = 12.sp, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            courses.forEachIndexed { idx, c ->
                                DropdownMenuItem(
                                    text = { Text(c.course.name) },
                                    onClick = {
                                        selectedCourseIndex = idx
                                        courseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Deadline Picker Trigger
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val formattedDate = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(selectedDeadlineMillis))
                        Text(formattedDate, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("输入任务内容...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            val cIndex = selectedCourseIndex ?: return@Button
                            val cId = courses[cIndex].course.id
                            val homework = Homework(
                                courseId = cId,
                                title = title,
                                deadline = selectedDeadlineMillis,
                                isCompleted = false
                            )
                            onAddHomework(homework)
                            title = "" // Clear input
                        },
                        enabled = title.isNotBlank() && selectedCourseIndex != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Tasks Checklist
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (uncompletedTasks.isNotEmpty()) {
                item {
                    Text("进行中", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(uncompletedTasks) { task ->
                    val associatedCourse = courses.find { it.course.id == task.courseId }?.course
                    TodoItemRow(
                        task = task,
                        courseName = associatedCourse?.name ?: "未知课程",
                        courseColorHex = associatedCourse?.colorHex ?: "#CCCCCC",
                        onCheckedChange = { isChecked ->
                            onUpdateHomework(task.copy(isCompleted = isChecked))
                        },
                        onDelete = { onDeleteHomework(task) }
                    )
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text("已完成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }

                items(completedTasks) { task ->
                    val associatedCourse = courses.find { it.course.id == task.courseId }?.course
                    TodoItemRow(
                        task = task,
                        courseName = associatedCourse?.name ?: "未知课程",
                        courseColorHex = associatedCourse?.colorHex ?: "#CCCCCC",
                        onCheckedChange = { isChecked ->
                            onUpdateHomework(task.copy(isCompleted = isChecked))
                        },
                        onDelete = { onDeleteHomework(task) }
                    )
                }
            }

            if (homeworkList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("当前没有任何待办任务！", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDeadlineMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDeadlineMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TodoItemRow(
    task: Homework,
    courseName: String,
    courseColorHex: String,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    val deadlineDate = Date(task.deadline)
    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val taskCalendar = Calendar.getInstance().apply {
        time = deadlineDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val daysDiff = ((taskCalendar.timeInMillis - todayCalendar.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

    val countdownText = when {
        task.isCompleted -> "已完成"
        daysDiff < 0 -> "已过期"
        daysDiff == 0 -> "今天截止"
        daysDiff == 1 -> "明天截止"
        else -> "剩 $daysDiff 天"
    }

    val countdownColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        daysDiff < 0 || daysDiff == 0 -> MaterialTheme.colorScheme.error
        daysDiff == 1 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    // Particle system states
    var isExploding by remember { mutableStateOf(false) }
    var particleTime by remember { mutableStateOf(0f) }

    val particles = remember {
        List(15) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (30f + Math.random() * 120f).toFloat()
            val size = (4f + Math.random() * 8f).toFloat()
            val color = when((0..4).random()) {
                0 -> Color(0xFF98A78F)
                1 -> Color(0xFF7B5455)
                2 -> Color(0xFF4F6071)
                3 -> Color(0xFFFDCBCB)
                else -> Color(0xFFD2F4EA)
            }
            Triple(angle, speed, size to color)
        }
    }

    if (isExploding) {
        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            val duration = 800f
            while (System.currentTimeMillis() - startTime < duration) {
                particleTime = (System.currentTimeMillis() - startTime) / duration
                kotlinx.coroutines.delay(16)
            }
            isExploding = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Particle explosion Canvas behind elements but inside card
            if (isExploding) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val centerX = 24.dp.toPx()
                    val centerY = size.height / 2f
                    particles.forEach { (angle, speed, sizeColor) ->
                        val (pSize, color) = sizeColor
                        val distance = speed * particleTime
                        val x = centerX + (Math.cos(angle) * distance).toFloat()
                        val y = centerY + (Math.sin(angle) * distance).toFloat()
                        val finalY = y + 80f * particleTime * particleTime // gravity
                        val alpha = 1f - particleTime
                        drawCircle(
                            color = color,
                            radius = pSize,
                            center = androidx.compose.ui.geometry.Offset(x, finalY),
                            alpha = alpha.coerceIn(0f, 1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BouncyCheckbox(
                    checked = task.isCompleted,
                    onCheckedChange = { isChecked ->
                        onCheckedChange(isChecked)
                        // Trigger explosion if checking to completed
                        if (isChecked) {
                            isExploding = true
                            particleTime = 0f
                        }
                        // Trigger Haptic Feedback
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(30)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Course Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(android.graphics.Color.parseColor(courseColorHex)))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = courseName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = getContrastingTextColor(courseColorHex, isSystemInDarkTheme())
                            )
                        }

                        // Deadline warning tag
                        Text(
                            text = countdownText,
                            fontSize = 11.sp,
                            color = countdownColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除任务",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun BouncyCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1.2f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "checkboxScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

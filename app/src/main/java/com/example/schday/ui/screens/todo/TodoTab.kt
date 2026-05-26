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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.Homework
import com.example.schday.theme.GlowTheme
import com.example.schday.R
import com.example.schday.theme.getContrastingTextColor
import com.example.schday.theme.glowOrShadow
import com.example.schday.theme.paperTexture
import com.example.schday.ui.components.GlowDatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTab(
    courses: List<CourseWithSchedules>,
    homeworkList: List<Homework>,
    appTheme: GlowTheme,
    onAddHomework: (Homework) -> Unit,
    onUpdateHomework: (Homework) -> Unit,
    onDeleteHomework: (Homework) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCourseIndex by remember { mutableStateOf<Int?>(null) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    // Date picker state
    val calendar = Calendar.getInstance()
    var selectedDeadlineMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val uncompletedTasks = remember(homeworkList) { homeworkList.filter { !it.isCompleted } }
    val completedTasks = remember(homeworkList) { homeworkList.filter { it.isCompleted } }

    val borderStroke = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.todo_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Quick Add Task Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .paperTexture(appTheme)
                .glowOrShadow(appTheme, isFeatured = false),
            shape = MaterialTheme.shapes.large,
            border = borderStroke,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.todo_quick_add), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                            shape = MaterialTheme.shapes.small
                        ) {
                            val cName = selectedCourseIndex?.let { courses.getOrNull(it)?.course?.name } ?: stringResource(R.string.todo_link_course)
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
                        shape = MaterialTheme.shapes.small
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
                        placeholder = { Text(stringResource(R.string.todo_input_placeholder)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
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
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.todo_add))
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
                    Text(stringResource(R.string.todo_in_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(uncompletedTasks) { task ->
                    val associatedCourse = courses.find { it.course.id == task.courseId }?.course
                    TodoItemRow(
                        task = task,
                        courseName = associatedCourse?.name ?: stringResource(R.string.todo_unknown_course),
                        courseColorHex = associatedCourse?.colorHex ?: "#CCCCCC",
                        appTheme = appTheme,
                        onCheckedChange = { isChecked ->
                            onUpdateHomework(task.copy(isCompleted = isChecked))
                        },
                        onDelete = { onDeleteHomework(task) }
                    )
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.todo_completed_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }

                items(completedTasks) { task ->
                    val associatedCourse = courses.find { it.course.id == task.courseId }?.course
                    TodoItemRow(
                        task = task,
                        courseName = associatedCourse?.name ?: stringResource(R.string.todo_unknown_course),
                        courseColorHex = associatedCourse?.colorHex ?: "#CCCCCC",
                        appTheme = appTheme,
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
                        Text(stringResource(R.string.todo_empty_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Theme-Aware Glow Date Picker Dialog
    if (showDatePicker) {
        GlowDatePickerDialog(
            initialDateMillis = selectedDeadlineMillis,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedTime ->
                selectedDeadlineMillis = selectedTime
                showDatePicker = false
            },
            appTheme = appTheme
        )
    }
}

@Composable
fun TodoItemRow(
    task: Homework,
    courseName: String,
    courseColorHex: String,
    appTheme: GlowTheme,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

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
        task.isCompleted -> stringResource(R.string.todo_status_completed)
        daysDiff < 0 -> stringResource(R.string.todo_status_overdue)
        daysDiff == 0 -> stringResource(R.string.todo_due_today)
        daysDiff == 1 -> stringResource(R.string.todo_due_tomorrow)
        else -> stringResource(R.string.todo_days_remaining, daysDiff)
    }

    val countdownColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        daysDiff < 0 || daysDiff == 0 -> MaterialTheme.colorScheme.error
        daysDiff == 1 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    val borderStroke = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    // Particle system states
    var isExploding by remember { mutableStateOf(false) }
    var particleTime by remember { mutableFloatStateOf(0f) }

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
        modifier = Modifier
            .fillMaxWidth()
            .paperTexture(appTheme)
            .glowOrShadow(appTheme, isFeatured = !task.isCompleted),
        shape = MaterialTheme.shapes.medium,
        border = borderStroke,
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
                    appTheme = appTheme,
                    onCheckedChange = { isChecked ->
                        onCheckedChange(isChecked)
                        // Trigger explosion if checking to completed
                        if (isChecked) {
                            isExploding = true
                            particleTime = 0f
                        }
                        // Trigger Haptic Feedback
                        com.example.schday.utils.DateUtils.triggerHapticFeedback(context, 30)
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
                        contentDescription = stringResource(R.string.todo_delete_task),
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
    appTheme: GlowTheme,
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

    val checkboxShape = when (appTheme) {
        GlowTheme.ACADEMIC_SERENITY -> RoundedCornerShape(6.dp)
        GlowTheme.DEEP_CHARCOAL -> RoundedCornerShape(4.dp)
        GlowTheme.AMOLED_POP -> RoundedCornerShape(2.dp)
        GlowTheme.VINTAGE_LIBRARY -> CircleShape
    }

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .size(24.dp)
            .clip(checkboxShape)
            .background(
                if (checked) {
                    if (appTheme == GlowTheme.VINTAGE_LIBRARY) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (appTheme == GlowTheme.AMOLED_POP) 1.5.dp else 2.dp,
                color = if (checked) {
                    if (appTheme == GlowTheme.VINTAGE_LIBRARY) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary
                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = checkboxShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (appTheme == GlowTheme.VINTAGE_LIBRARY) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

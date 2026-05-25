package com.example.schday.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.data.entity.Semester
import com.example.schday.theme.getContrastingTextColor
import com.example.schday.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeTab(
    semester: Semester?,
    courses: List<CourseWithSchedules>,
    periods: List<PeriodTime>,
    onAddCourseClick: () -> Unit
) {
    if (semester == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先去设置页面创建一个学期！", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
    val dayOfWeek = DateUtils.getDayOfWeek()
    val todayDateStr = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date())
    val todayDayName = DateUtils.getDayName(dayOfWeek)

    // Compute active courses for today
    val todaySlots = remember(courses, currentWeek, dayOfWeek) {
        val list = mutableListOf<Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>>()
        for (c in courses) {
            for (s in c.slots) {
                if (s.dayOfWeek == dayOfWeek && DateUtils.isWeekActive(s.activeWeeks, currentWeek)) {
                    val p = periods.find { it.periodNumber == s.startPeriod }
                    list.add(Triple(c, s, p))
                }
            }
        }
        list.sortBy { it.second.startPeriod }
        list
    }

    // Timer or clock state to update countdown every minute
    var timeMinutes by remember { mutableStateOf(getCurrentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15000) // check every 15s
            timeMinutes = getCurrentMinutes()
        }
    }

    // Compute countdown status
    val countdownStatus = remember(todaySlots, periods, timeMinutes) {
        computeCountdown(todaySlots, periods, timeMinutes)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCourseClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
        ) {
            // Welcome Header
            item {
                Column {
                    Text(
                        text = semester.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "第 $currentWeek 周 $todayDayName • $todayDateStr",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Countdown Card
            item {
                CountdownCard(status = countdownStatus)
            }

            // Timeline Header
            item {
                Text(
                    text = "今日课程日程",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (todaySlots.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎉", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("今天没课！快去享受自由时光吧～", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                items(todaySlots) { (courseWithSchedules, slot, startPeriodTime) ->
                    val endPeriodTime = periods.find { it.periodNumber == slot.endPeriod }
                    val startTimeStr = startPeriodTime?.startTime ?: ""
                    val endTimeStr = endPeriodTime?.endTime ?: ""

                    val isPast = remember(startTimeStr, endTimeStr, timeMinutes) {
                        if (endTimeStr.isEmpty()) false
                        else timeStringToMinutes(endTimeStr) < timeMinutes
                    }

                    val isActive = remember(startTimeStr, endTimeStr, timeMinutes) {
                        if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) false
                        else {
                            val startMin = timeStringToMinutes(startTimeStr)
                            val endMin = timeStringToMinutes(endTimeStr)
                            timeMinutes in startMin..endMin
                        }
                    }

                    CourseTimelineItem(
                        courseName = courseWithSchedules.course.name,
                        teacher = courseWithSchedules.course.teacher,
                        classroom = slot.classroom,
                        startPeriod = slot.startPeriod,
                        endPeriod = slot.endPeriod,
                        startTime = startTimeStr,
                        endTime = endTimeStr,
                        colorHex = courseWithSchedules.course.colorHex,
                        isPast = isPast,
                        isActive = isActive,
                        hasPendingHomework = courseWithSchedules.homework.any { !it.isCompleted }
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownCard(status: CountdownStatus) {
    val gradientBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF8E99F3), Color(0xFF6F74D2))
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (status.isActive) Icons.Default.PlayArrow else Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = if (status.isActive) "正在上课" else if (status.courseName != null) "下堂课预告" else "课程结束",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status.courseName != null) {
                    Text(
                        text = status.courseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${status.startTime}-${status.endTime} (${status.classroom})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = status.countdownText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "今天没有更多课程了",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "呼～今天的课全部搞定！开启美好的课后时光吧 🥳",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun CourseTimelineItem(
    courseName: String,
    teacher: String,
    classroom: String,
    startPeriod: Int,
    endPeriod: Int,
    startTime: String,
    endTime: String,
    colorHex: String,
    isPast: Boolean,
    isActive: Boolean,
    hasPendingHomework: Boolean = false
) {
    val cardColor = Color(android.graphics.Color.parseColor(colorHex))
    val textColor = getContrastingTextColor(colorHex, isSystemInDarkTheme())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.5f else 1.0f),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time indicator
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Timeline Node
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // Course Card
        Card(
            modifier = Modifier
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(20.dp),
            border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasPendingHomework) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📝 待办",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Red
                            )
                        }
                    }
                }
                if (teacher.isNotEmpty() || classroom.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = listOfNotNull(
                                classroom.takeIf { it.isNotEmpty() },
                                teacher.takeIf { it.isNotEmpty() }
                            ).joinToString(" | "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
                Text(
                    text = "第 $startPeriod-$endPeriod 节",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

data class CountdownStatus(
    val courseName: String?,
    val startTime: String,
    val endTime: String,
    val classroom: String,
    val isActive: Boolean,
    val countdownText: String
)

private fun computeCountdown(
    todaySlots: List<Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>>,
    periods: List<PeriodTime>,
    timeMinutes: Int
): CountdownStatus {
    var activeSlot: Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>? = null
    var nextSlot: Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>? = null

    for (item in todaySlots) {
        val slot = item.second
        val startP = periods.find { it.periodNumber == slot.startPeriod }
        val endP = periods.find { it.periodNumber == slot.endPeriod }
        val startMin = timeStringToMinutes(startP?.startTime ?: "")
        val endMin = timeStringToMinutes(endP?.endTime ?: "")

        if (timeMinutes in startMin..endMin) {
            activeSlot = item
            break
        } else if (startMin > timeMinutes) {
            if (nextSlot == null) {
                nextSlot = item
            }
        }
    }

    if (activeSlot != null) {
        val slot = activeSlot.second
        val startP = periods.find { it.periodNumber == slot.startPeriod }
        val endP = periods.find { it.periodNumber == slot.endPeriod }
        val endMin = timeStringToMinutes(endP?.endTime ?: "")
        val diff = endMin - timeMinutes
        return CountdownStatus(
            courseName = activeSlot.first.course.name,
            startTime = startP?.startTime ?: "",
            endTime = endP?.endTime ?: "",
            classroom = slot.classroom,
            isActive = true,
            countdownText = "下课倒计时：还剩 $diff 分钟，加油！"
        )
    } else if (nextSlot != null) {
        val slot = nextSlot.second
        val startP = periods.find { it.periodNumber == slot.startPeriod }
        val endP = periods.find { it.periodNumber == slot.endPeriod }
        val startMin = timeStringToMinutes(startP?.startTime ?: "")
        val diff = startMin - timeMinutes
        return CountdownStatus(
            courseName = nextSlot.first.course.name,
            startTime = startP?.startTime ?: "",
            endTime = endP?.endTime ?: "",
            classroom = slot.classroom,
            isActive = false,
            countdownText = "下堂课准备中：还剩 $diff 分钟上课，别迟到哦 🏃‍♂️"
        )
    }

    return CountdownStatus(
        courseName = null,
        startTime = "",
        endTime = "",
        classroom = "",
        isActive = false,
        countdownText = ""
    )
}

private fun getCurrentMinutes(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

private fun timeStringToMinutes(timeStr: String): Int {
    if (timeStr.isEmpty()) return 0
    val parts = timeStr.split(":")
    if (parts.size != 2) return 0
    val hour = parts[0].toIntOrNull() ?: 0
    val minute = parts[1].toIntOrNull() ?: 0
    return hour * 60 + minute
}

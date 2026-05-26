package com.example.schday.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.schday.data.AppDatabase
import com.example.schday.data.DefaultDataRepository
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.data.entity.Semester
import com.example.schday.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = DefaultDataRepository(db)
        
        val semester = repository.getCurrentSemester().first()
        val courses = semester?.let { repository.getCoursesBySemester(it.id).first() } ?: emptyList()
        val periods = repository.getAllPeriodTimes().first()

        provideContent {
            GlanceTheme {
                WidgetContent(semester, courses, periods)
            }
        }
    }

    private data class WidgetSlotItem(
        val courseWithSchedules: CourseWithSchedules,
        val slot: ScheduleSlot,
        val periodTime: PeriodTime?,
        val isActive: Boolean,
        val isUpcoming: Boolean,
        val startMillis: Long,
        val endMillis: Long
    )

    private fun parseTimeStringToday(timeStr: String): Long {
        val calendar = Calendar.getInstance()
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        return 0L
    }

    @Composable
    private fun WidgetContent(
        semester: Semester?,
        courses: List<CourseWithSchedules>,
        periods: List<PeriodTime>
    ) {
        if (semester == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .background(ImageProvider(com.example.schday.R.drawable.widget_glass_background)),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无当前学期信息", style = TextStyle(color = GlanceTheme.colors.onBackground))
            }
            return
        }

        val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
        val dayOfWeek = DateUtils.getDayOfWeek()

        val todaySlots = courses.flatMap { c ->
            c.slots.filter { s ->
                s.dayOfWeek == dayOfWeek && DateUtils.isWeekActive(s.activeWeeks, currentWeek)
            }.map { s ->
                val p = periods.find { it.periodNumber == s.startPeriod }
                Triple(c, s, p)
            }
        }.sortedBy { it.second.startPeriod }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.example.schday.R.drawable.widget_glass_background))
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日课表",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontWeight = androidx.glance.text.FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "第 $currentWeek 周",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))

                if (todaySlots.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("今天没有课程哦！", style = TextStyle(color = GlanceTheme.colors.onBackground))
                    }
                } else {
                    val now = System.currentTimeMillis()
                    var activeIndex = -1
                    val processedSlots = todaySlots.mapIndexed { index, (courseWithSchedules, slot, periodTime) ->
                        val startStr = periodTime?.startTime ?: "00:00"
                        val endStr = periodTime?.endTime ?: "00:00"
                        val startMillis = parseTimeStringToday(startStr)
                        val endMillis = parseTimeStringToday(endStr)
                        
                        val isActive = now in startMillis..endMillis
                        val isUpcoming = startMillis > now
                        
                        if (isActive && activeIndex == -1) {
                            activeIndex = index
                        }
                        
                        WidgetSlotItem(
                            courseWithSchedules = courseWithSchedules,
                            slot = slot,
                            periodTime = periodTime,
                            isActive = isActive,
                            isUpcoming = isUpcoming,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    }

                    // Select the main item to display
                    val mainItem = if (activeIndex != -1) {
                        processedSlots[activeIndex]
                    } else {
                        // Find the first upcoming class, or fall back to the first class of the day
                        processedSlots.firstOrNull { it.isUpcoming } ?: processedSlots.first()
                    }

                    // Main card showing Active or Next class
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(com.example.schday.R.drawable.card_sage_background))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mainItem.isActive) "进行中" else "下一节",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontWeight = androidx.glance.text.FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${mainItem.periodTime?.startTime ?: ""} - ${mainItem.periodTime?.endTime ?: ""}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = mainItem.courseWithSchedules.course.name,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontWeight = androidx.glance.text.FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "教室: ${mainItem.slot.classroom}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 11.sp
                                )
                            )
                            if (mainItem.courseWithSchedules.course.teacher.isNotBlank()) {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = "教师: ${mainItem.courseWithSchedules.course.teacher}",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onPrimaryContainer,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Progress Indicator if class is currently running
                        if (mainItem.isActive) {
                            val duration = mainItem.endMillis - mainItem.startMillis
                            val elapsed = now - mainItem.startMillis
                            val progress = if (duration > 0) elapsed.toFloat() / duration.toFloat() else 0f
                            val clampedProgress = progress.coerceIn(0f, 1f)
                            
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = clampedProgress,
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                        }
                    }

                    // Display the subsequently upcoming class in a secondary card if available
                    val nextItems = processedSlots.filter { it != mainItem && it.startMillis > now }
                    if (nextItems.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        val nextOne = nextItems.first()
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ImageProvider(com.example.schday.R.drawable.card_slate_background))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "随后: ${nextOne.courseWithSchedules.course.name}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onTertiaryContainer,
                                    fontWeight = androidx.glance.text.FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${nextOne.periodTime?.startTime ?: ""} @ ${nextOne.slot.classroom}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onTertiaryContainer,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

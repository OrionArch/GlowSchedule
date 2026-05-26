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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import java.util.Calendar

import android.util.Log
import androidx.glance.text.FontWeight

class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = DefaultDataRepository(db)
        
        val semester = repository.getCurrentSemester().first()
        val courses = semester?.let { repository.getCoursesBySemester(it.id).first() } ?: emptyList()
        val periods = repository.getAllPeriodTimes().first()

        Log.d("ScheduleWidget", "provideGlance: semester=${semester?.name}, coursesCount=${courses.size}, periodsCount=${periods.size}")

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
                Text("暂无当前学期信息", style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp))
            }
            return
        }

        val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
        val dayOfWeek = DateUtils.getDayOfWeek()

        Log.d("ScheduleWidget", "WidgetContent: currentWeek=$currentWeek, dayOfWeek=$dayOfWeek")

        val todaySlots = courses.flatMap { c ->
            c.slots.filter { s ->
                val dayMatch = s.dayOfWeek == dayOfWeek
                val weekActive = DateUtils.isWeekActive(s.activeWeeks, currentWeek)
                Log.d("ScheduleWidget", "Checking course '${c.course.name}' slot dayOfWeek=${s.dayOfWeek} (match=$dayMatch), activeWeeks='${s.activeWeeks}' (active=$weekActive)")
                dayMatch && weekActive
            }.map { s ->
                val p = periods.find { it.periodNumber == s.startPeriod }
                Triple(c, s, p)
            }
        }.sortedBy { it.second.startPeriod }

        Log.d("ScheduleWidget", "WidgetContent: todaySlotsCount=${todaySlots.size}")

        if (todaySlots.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .background(ImageProvider(com.example.schday.R.drawable.widget_glass_background))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(ImageProvider(com.example.schday.R.drawable.card_slate_background))
                    ) {}
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(
                        modifier = GlanceModifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "今日无课",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = androidx.glance.text.FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "享受美好的一天吧！",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            return
        }

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

        val mainItem = if (activeIndex != -1) {
            processedSlots[activeIndex]
        } else {
            processedSlots.firstOrNull { it.isUpcoming } ?: processedSlots.first()
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.example.schday.R.drawable.widget_glass_background))
                .padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barDrawable = if (mainItem.isActive) {
                    com.example.schday.R.drawable.card_sage_background
                } else {
                    com.example.schday.R.drawable.card_slate_background
                }
                Box(
                    modifier = GlanceModifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(ImageProvider(barDrawable))
                ) {}
                
                Spacer(modifier = GlanceModifier.width(8.dp))
                
                Column(
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stateText = if (mainItem.isActive) "进行中" else "下一节"
                        val stateColor = if (mainItem.isActive) {
                            GlanceTheme.colors.primary
                        } else {
                            GlanceTheme.colors.secondary
                        }
                        Text(
                            text = stateText,
                            style = TextStyle(
                                color = stateColor,
                                fontWeight = androidx.glance.text.FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = "${mainItem.periodTime?.startTime ?: ""} - ${mainItem.periodTime?.endTime ?: ""}",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "W$currentWeek",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 9.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    
                    Text(
                        text = mainItem.courseWithSchedules.course.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontWeight = androidx.glance.text.FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    
                    if (mainItem.isActive) {
                        val duration = mainItem.endMillis - mainItem.startMillis
                        val elapsed = now - mainItem.startMillis
                        val progress = if (duration > 0) elapsed.toFloat() / duration.toFloat() else 0f
                        val clampedProgress = progress.coerceIn(0f, 1f)
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "教室: ${mainItem.slot.classroom}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.secondary,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = clampedProgress,
                                modifier = GlanceModifier.defaultWeight().height(3.dp)
                            )
                        }
                    } else {
                        val teacherStr = if (mainItem.courseWithSchedules.course.teacher.isNotBlank()) {
                            " | 教师: ${mainItem.courseWithSchedules.course.teacher}"
                        } else {
                            ""
                        }
                        Text(
                            text = "教室: ${mainItem.slot.classroom}$teacherStr",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    companion object {
        fun updateWidget(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ScheduleWidget().updateAll(context)
                    Log.d("ScheduleWidget", "Widget updateAll triggered successfully")
                } catch (e: Exception) {
                    Log.e("ScheduleWidget", "Failed to update widget", e)
                }
            }
        }
    }
}

package com.example.schday.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.schday.data.AppDatabase
import com.example.schday.data.DefaultDataRepository
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.Semester
import com.example.schday.utils.DateUtils
import kotlinx.coroutines.flow.first

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

    @Composable
    private fun WidgetContent(
        semester: Semester?,
        courses: List<CourseWithSchedules>,
        periods: List<PeriodTime>
    ) {
        if (semester == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background),
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

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp)
        ) {
            Text(
                text = "今日课表 (第 $currentWeek 周)",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = androidx.glance.text.FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (todaySlots.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("今天没有课程哦！", style = TextStyle(color = GlanceTheme.colors.onBackground))
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    todaySlots.take(3).forEach { (courseWithSchedules, slot, periodTime) ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${periodTime?.startTime ?: ""} ",
                                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp)
                            )
                            Column {
                                Text(
                                    text = courseWithSchedules.course.name,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onBackground,
                                        fontWeight = androidx.glance.text.FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "教室: ${slot.classroom}",
                                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 10.sp)
                                )
                            }
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

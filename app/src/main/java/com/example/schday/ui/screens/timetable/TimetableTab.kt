package com.example.schday.ui.screens.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.Semester
import com.example.schday.theme.getContrastingTextColor
import com.example.schday.utils.DateUtils
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTab(
    semester: Semester?,
    courses: List<CourseWithSchedules>,
    periods: List<PeriodTime>,
    selectedWeek: Int?,
    onWeekSelected: (Int) -> Unit,
    onCourseClick: (Int) -> Unit
) {
    if (semester == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先去设置页面创建一个学期！", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
    val coroutineScope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(
        initialPage = ((selectedWeek ?: currentWeek) - 1).coerceIn(0, semester.totalWeeks - 1),
        pageCount = { semester.totalWeeks }
    )

    // Sync from ViewModel selectedWeek to PagerState page
    LaunchedEffect(selectedWeek) {
        selectedWeek?.let { week ->
            val targetPage = week - 1
            if (pagerState.currentPage != targetPage && targetPage in 0 until semester.totalWeeks) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    // Sync from PagerState page to ViewModel selectedWeek
    LaunchedEffect(pagerState.currentPage) {
        onWeekSelected(pagerState.currentPage + 1)
    }

    val activeWeek = pagerState.currentPage + 1

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE) }
    var showOnlyCurrentWeek by remember { mutableStateOf(false) }
    var hideWeekends by remember { mutableStateOf(sharedPreferences.getBoolean("hide_weekends", false)) }
    var weekDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Week Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    InputChip(
                        selected = true,
                        onClick = { weekDropdownExpanded = true },
                        label = { 
                            Text(
                                text = "第 $activeWeek 周" + if (activeWeek == currentWeek) " (本周)" else "",
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                    )

                    DropdownMenu(
                        expanded = weekDropdownExpanded,
                        onDismissRequest = { weekDropdownExpanded = false }
                    ) {
                        (1..semester.totalWeeks).forEach { week ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "第 $week 周" + if (week == currentWeek) " (本周)" else "",
                                        fontWeight = if (week == activeWeek) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(week - 1)
                                    }
                                    weekDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (activeWeek != currentWeek) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentWeek - 1)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("回到本周", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show Only Current Week Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("只显本周", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = showOnlyCurrentWeek,
                        onCheckedChange = { showOnlyCurrentWeek = it },
                        modifier = Modifier.scale(0.7f)
                    )
                }

                // Hide Weekends Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("隐藏周末", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = hideWeekends,
                        onCheckedChange = { checked ->
                            hideWeekends = checked
                            sharedPreferences.edit().putBoolean("hide_weekends", checked).apply()
                        },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }

        // Horizontal Pager that wraps DaysHeaderRow and TimetableGrid
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val pageWeek = page + 1
            Column(modifier = Modifier.fillMaxSize()) {
                // Days Header Row (Mon..Sun)
                DaysHeaderRow(
                    currentDayOfWeek = DateUtils.getDayOfWeek(), 
                    isCurrentWeek = pageWeek == currentWeek,
                    hideWeekends = hideWeekends
                )

                // Grid Area with vertical scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    TimetableGrid(
                        courses = courses,
                        periods = periods,
                        activeWeek = pageWeek,
                        showOnlyCurrentWeek = showOnlyCurrentWeek,
                        hideWeekends = hideWeekends,
                        onCourseClick = onCourseClick
                    )
                }
            }
        }
    }
}

// Extends Modifier to scale for smaller elements (like the switch)
fun Modifier.scale(scale: Float): Modifier = this.then(
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
)

@Composable
fun DaysHeaderRow(currentDayOfWeek: Int, isCurrentWeek: Boolean, hideWeekends: Boolean) {
    val days = if (hideWeekends) {
        listOf("周一", "周二", "周三", "周四", "周五")
    } else {
        listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Space for time column
        Spacer(modifier = Modifier.width(40.dp))

        days.forEachIndexed { index, day ->
            val isToday = isCurrentWeek && (index + 1 == currentDayOfWeek)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
fun TimetableGrid(
    courses: List<CourseWithSchedules>,
    periods: List<PeriodTime>,
    activeWeek: Int,
    showOnlyCurrentWeek: Boolean,
    hideWeekends: Boolean,
    onCourseClick: (Int) -> Unit
) {
    val rowHeight = 62.dp
    val leftHeaderWidth = 40.dp
    val totalPeriods = 12
    val totalCols = if (hideWeekends) 5 else 7

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight * totalPeriods)
    ) {
        val colWidth = (maxWidth - leftHeaderWidth) / totalCols

        // 1. Draw Grid Lines and Period Numbers
        for (i in 0 until totalPeriods) {
            val period = periods.find { it.periodNumber == i + 1 }
            val yOffset = rowHeight * i

            // Horizontal grid line
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = yOffset),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // Period header on the left
            Column(
                modifier = Modifier
                    .width(leftHeaderWidth)
                    .height(rowHeight)
                    .offset(y = yOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${i + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                if (period != null) {
                    Text(
                        text = period.startTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Draw vertical columns separators
        for (col in 0..totalCols) {
            val xOffset = leftHeaderWidth + colWidth * col
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .fillMaxHeight()
                    .offset(x = xOffset)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }

        // 2. Render Course Cards
        courses.forEach { courseWithSchedules ->
            val course = courseWithSchedules.course
            val hasPendingHomework = courseWithSchedules.homework.any { !it.isCompleted }
            courseWithSchedules.slots.forEach { slot ->
                if (hideWeekends && slot.dayOfWeek > 5) {
                    return@forEach
                }
                val isActive = DateUtils.isWeekActive(slot.activeWeeks, activeWeek)
                if (showOnlyCurrentWeek && !isActive) {
                    return@forEach // Skip drawing if filtered out
                }

                val startP = slot.startPeriod
                val endP = slot.endPeriod
                val duration = endP - startP + 1

                val cardColor = Color(android.graphics.Color.parseColor(course.colorHex))
                val textColor = getContrastingTextColor(course.colorHex, isSystemInDarkTheme())

                val cardX = leftHeaderWidth + colWidth * (slot.dayOfWeek - 1)
                val cardY = rowHeight * (startP - 1)
                val cardWidth = colWidth
                val cardHeight = rowHeight * duration

                Box(
                    modifier = Modifier
                        .offset(x = cardX, y = cardY)
                        .width(cardWidth)
                        .height(cardHeight)
                        .padding(2.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onCourseClick(course.id) }
                            .alpha(if (isActive) 1f else 0.45f),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(8.dp),
                        border = if (!isActive) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                fontSize = 11.sp
                            )
                            if (slot.classroom.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "@" + slot.classroom,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 10.sp
                                )
                            }
                            if (!isActive) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "非本周",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Top-right Red Dot Badge for Homework Todo items
                    if (hasPendingHomework) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(6.dp)
                                .background(Color.Red, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}

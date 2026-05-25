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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
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

data class DisplaySlot(
    val course: com.example.schday.data.entity.Course,
    val slot: com.example.schday.data.entity.ScheduleSlot,
    val hasPendingHomework: Boolean,
    val isActive: Boolean,
    val originalCourseWithSchedules: CourseWithSchedules
)

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

    // BottomSheet states for course details and conflict resolution
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedCourseGroup by remember { mutableStateOf<List<DisplaySlot>>(emptyList()) }
    var activeDetailsIndex by remember { mutableStateOf(0) }
    val topCourseIds = remember { mutableStateMapOf<String, Int>() }

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
                        topCourseIds = topCourseIds,
                        onCourseClick = { group ->
                            selectedCourseGroup = group
                            activeDetailsIndex = 0
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Course Details / Conflicts
    if (showBottomSheet && selectedCourseGroup.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            val currentDetailSlot = selectedCourseGroup.getOrNull(activeDetailsIndex)
            if (currentDetailSlot != null) {
                val course = currentDetailSlot.course
                val slot = currentDetailSlot.slot

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header / Course Selector if there's conflict
                    if (selectedCourseGroup.size > 1) {
                        Text(
                            text = "⚠️ 该时段有 ${selectedCourseGroup.size} 门冲突课程",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ScrollableTabRow(
                            selectedTabIndex = activeDetailsIndex,
                            edgePadding = 0.dp,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            selectedCourseGroup.forEachIndexed { index, displaySlot ->
                                Tab(
                                    selected = activeDetailsIndex == index,
                                    onClick = { activeDetailsIndex = index },
                                    text = { 
                                        Text(
                                            displaySlot.course.name, 
                                            maxLines = 1, 
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 13.sp,
                                            fontWeight = if (activeDetailsIndex == index) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Course Title with Morandi color dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(android.graphics.Color.parseColor(course.colorHex)), RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailItem(icon = Icons.Default.Person, label = "任课教师", value = course.teacher.ifEmpty { "未指定" })
                        DetailItem(
                            icon = Icons.Default.Place, 
                            label = "上课教室", 
                            value = slot.classroom.ifEmpty { "未指定" }
                        )
                        val dayStr = when(slot.dayOfWeek) {
                            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
                        }
                        val timePeriod = periods.find { it.periodNumber == slot.startPeriod }
                        val endPeriod = periods.find { it.periodNumber == slot.endPeriod }
                        val timeStr = if (timePeriod != null && endPeriod != null) {
                            "$dayStr 第 ${slot.startPeriod}-${slot.endPeriod} 节 (${timePeriod.startTime} - ${endPeriod.endTime})"
                        } else {
                            "$dayStr 第 ${slot.startPeriod}-${slot.endPeriod} 节"
                        }
                        DetailItem(icon = Icons.Default.DateRange, label = "上课时间", value = timeStr)
                        DetailItem(icon = Icons.Default.Info, label = "上课周数", value = "第 ${slot.activeWeeks} 周")
                    }

                    // Associated Homework
                    val homework = currentDetailSlot.originalCourseWithSchedules.homework
                    if (homework.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "📋 关联待办",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                homework.forEach { hw ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = hw.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (hw.isCompleted) TextDecoration.LineThrough else null,
                                            color = if (hw.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (hw.isCompleted) "已完成" else "待完成",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hw.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedCourseGroup.size > 1) {
                            Button(
                                onClick = {
                                    val key = "${slot.dayOfWeek}-${slot.startPeriod}"
                                    topCourseIds[key] = course.id
                                    showBottomSheet = false
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("置顶显示")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                showBottomSheet = false
                                onCourseClick(course.id)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("编辑课程")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
    topCourseIds: Map<String, Int>,
    onCourseClick: (List<DisplaySlot>) -> Unit
) {
    val rowHeight = 62.dp
    val leftHeaderWidth = 40.dp
    val totalPeriods = 12
    val totalCols = if (hideWeekends) 5 else 7

    // 1. Gather and preprocess slots
    val displaySlots = mutableListOf<DisplaySlot>()
    courses.forEach { courseWithSchedules ->
        val course = courseWithSchedules.course
        val hasPendingHomework = courseWithSchedules.homework.any { !it.isCompleted }
        courseWithSchedules.slots.forEach { slot ->
            if (hideWeekends && slot.dayOfWeek > 5) {
                return@forEach
            }
            val isActive = DateUtils.isWeekActive(slot.activeWeeks, activeWeek)
            if (showOnlyCurrentWeek && !isActive) {
                return@forEach
            }
            displaySlots.add(
                DisplaySlot(
                    course = course,
                    slot = slot,
                    hasPendingHomework = hasPendingHomework,
                    isActive = isActive,
                    originalCourseWithSchedules = courseWithSchedules
                )
            )
        }
    }

    // Group overlapping slots for conflict overlay
    // Overlap condition: same dayOfWeek and overlap in periods
    val groupedSlots = mutableListOf<MutableList<DisplaySlot>>()
    displaySlots.forEach { item ->
        var added = false
        for (group in groupedSlots) {
            if (group.any { g ->
                g.slot.dayOfWeek == item.slot.dayOfWeek &&
                maxOf(g.slot.startPeriod, item.slot.startPeriod) <= minOf(g.slot.endPeriod, item.slot.endPeriod)
            }) {
                group.add(item)
                added = true
                break
            }
        }
        if (!added) {
            groupedSlots.add(mutableListOf(item))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight * totalPeriods)
    ) {
        val colWidth = (maxWidth - leftHeaderWidth) / totalCols

        // Draw Grid Lines and Period Numbers
        for (i in 0 until totalPeriods) {
            val period = periods.find { it.periodNumber == i + 1 }
            val yOffset = rowHeight * i

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = yOffset),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

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

        // Render groups
        groupedSlots.forEach { group ->
            // Sort group so that the top selected course is rendered last (front of stack)
            val key = "${group[0].slot.dayOfWeek}-${group[0].slot.startPeriod}"
            val topCourseId = topCourseIds[key]
            
            val sortedGroup = group.sortedWith { a, b ->
                when {
                    a.course.id == topCourseId && b.course.id != topCourseId -> 1
                    b.course.id == topCourseId && a.course.id != topCourseId -> -1
                    a.isActive && !b.isActive -> 1
                    !a.isActive && b.isActive -> -1
                    else -> a.course.id.compareTo(b.course.id)
                }
            }

            sortedGroup.forEachIndexed { index, displaySlot ->
                val course = displaySlot.course
                val slot = displaySlot.slot
                val isActive = displaySlot.isActive
                val hasPendingHomework = displaySlot.hasPendingHomework

                val startP = slot.startPeriod
                val endP = slot.endPeriod
                val duration = endP - startP + 1

                val cardColor = Color(android.graphics.Color.parseColor(course.colorHex))
                val textColor = getContrastingTextColor(course.colorHex, isSystemInDarkTheme())

                // Adjust positioning and sizing for 3D card deck effect
                val totalInGroup = sortedGroup.size
                val offsetStep = 4.dp
                val stackOffset = (index * offsetStep.value).dp

                val cardX = leftHeaderWidth + colWidth * (slot.dayOfWeek - 1) + stackOffset
                val cardY = rowHeight * (startP - 1) + stackOffset
                val cardWidth = colWidth - ((totalInGroup - 1) * offsetStep.value).dp
                val cardHeight = rowHeight * duration - ((totalInGroup - 1) * offsetStep.value).dp

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
                            .clickable { onCourseClick(sortedGroup) }
                            .alpha(if (isActive) 1f else 0.45f),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(8.dp),
                        border = if (!isActive) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) 
                                 else BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
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

package com.example.schday.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.content.Context
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset

import com.example.schday.R
import com.example.schday.data.entity.CourseWithSchedules
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.data.entity.Semester
import com.example.schday.theme.GlowTheme
import com.example.schday.theme.getContrastingTextColor
import com.example.schday.theme.glowOrShadow
import com.example.schday.theme.paperTexture
import com.example.schday.theme.GlowDivider
import com.example.schday.theme.bounceClickable
import com.example.schday.theme.interactiveTilt
import com.example.schday.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeTab(
    semester: Semester?,
    courses: List<CourseWithSchedules>,
    periods: List<PeriodTime>,
    appTheme: GlowTheme,
    onAddCourseClick: () -> Unit
) {
    if (semester == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_semester_hint), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val context = LocalContext.current
    val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
    val dayOfWeek = DateUtils.getDayOfWeek()
    val dateFormatStr = stringResource(R.string.home_date_format)
    val todayDateStr = SimpleDateFormat(dateFormatStr, Locale.getDefault()).format(Date())
    val todayDayName = DateUtils.getDayName(context, dayOfWeek)

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
    var timeMinutes by remember { mutableIntStateOf(getCurrentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15000) // check every 15s
            timeMinutes = getCurrentMinutes()
        }
    }

    // Compute countdown status
    val countdownStatus = remember(todaySlots, periods, timeMinutes) {
        computeCountdown(todaySlots, periods, timeMinutes, context)
    }

    val sharedPreferences = remember(context) { context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE) }

    // Find active, next, and recently finished slots
    val activeSlot = remember(todaySlots, periods, timeMinutes) {
        var active: Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>? = null
        for (item in todaySlots) {
            val slot = item.second
            val startP = periods.find { it.periodNumber == slot.startPeriod }
            val endP = periods.find { it.periodNumber == slot.endPeriod }
            val startMin = timeStringToMinutes(startP?.startTime ?: "")
            val endMin = timeStringToMinutes(endP?.endTime ?: "")
            if (timeMinutes in startMin..endMin) {
                active = item
                break
            }
        }
        active
    }

    val nextSlot = remember(todaySlots, periods, timeMinutes) {
        var next: Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>? = null
        for (item in todaySlots) {
            val slot = item.second
            val startP = periods.find { it.periodNumber == slot.startPeriod }
            val startMin = timeStringToMinutes(startP?.startTime ?: "")
            if (startMin > timeMinutes) {
                next = item
                break
            }
        }
        next
    }

    val recentlyFinishedSlot = remember(todaySlots, periods, timeMinutes) {
        var finished: Triple<CourseWithSchedules, ScheduleSlot, PeriodTime?>? = null
        for (item in todaySlots) {
            val slot = item.second
            val endP = periods.find { it.periodNumber == slot.endPeriod }
            val endMin = timeStringToMinutes(endP?.endTime ?: "")
            if (endMin < timeMinutes && (timeMinutes - endMin) <= 30) {
                finished = item
                break
            }
        }
        finished
    }

    val isDark = isSystemInDarkTheme()
    val blobColor1 = if (isDark) Color(0x0F34D399) else Color(0x1B34D399) // Mint/emerald
    val blobColor2 = if (isDark) Color(0x0FFB7185) else Color(0x1BFB7185) // Rose/pink

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor1, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobColor2, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddCourseClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_course))
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
                // Redesigned Welcome Header with Pixel Cat Mascot
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hi, Jane!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_today_is, todayDateStr),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        // Mascot badge
                        var showSpeechBubble by remember { mutableStateOf(false) }
                        var speechText by remember { mutableStateOf("") }
                        val workloadCount = todaySlots.size

                        val mascotArt = when {
                            workloadCount == 0 -> "/\\_/\\\n( -.- )\n > Z <" // sleeping
                            workloadCount in 1..2 -> "/\\_/\\\n( o.o )\n > ^ <" // normal
                            workloadCount in 3..4 -> "/\\_/\\\n( ✪.✪ )\n > ✿ <" // focused
                            else -> "/\\_/\\\n( >.< )\n > ~ <" // overworked
                        }

                        val mascotMood = when {
                            workloadCount == 0 -> stringResource(R.string.home_mascot_sleeping)
                            workloadCount in 1..2 -> stringResource(R.string.home_mascot_energetic)
                            workloadCount in 3..4 -> stringResource(R.string.home_mascot_focused)
                            else -> stringResource(R.string.home_mascot_overworked)
                        }

                        val speechQuotes = listOf(
                            stringResource(R.string.home_quote_1),
                            stringResource(R.string.home_quote_2),
                            stringResource(R.string.home_quote_3),
                            stringResource(R.string.home_quote_4),
                            stringResource(R.string.home_quote_5),
                            stringResource(R.string.home_quote_6, workloadCount),
                            stringResource(R.string.home_quote_7),
                            stringResource(R.string.home_quote_8)
                        )

                        Box(contentAlignment = Alignment.TopEnd) {
                            Column(
                                modifier = Modifier
                                    .interactiveTilt(appTheme)
                                    .bounceClickable {
                                        val idx = (speechQuotes.indices).random()
                                        speechText = speechQuotes[idx]
                                        showSpeechBubble = true
                                        DateUtils.triggerHapticFeedback(context, 20)
                                    }
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = if (appTheme == GlowTheme.VINTAGE_LIBRARY) 0.5.dp else 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mascotArt,
                                    fontSize = 8.sp,
                                    lineHeight = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.home_mascot_prefix, mascotMood),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            if (showSpeechBubble) {
                                val yOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { (-65).dp.roundToPx() }
                                androidx.compose.ui.window.Popup(
                                    alignment = Alignment.TopCenter,
                                    offset = androidx.compose.ui.unit.IntOffset(0, yOffsetPx),
                                    onDismissRequest = { showSpeechBubble = false }
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .bounceClickable { showSpeechBubble = false },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Text(
                                            text = speechText,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Countdown Card
                item {
                    CountdownCard(status = countdownStatus, appTheme = appTheme)
                }

                // Custom experimental widgets
                item {
                    DndWidget(sharedPreferences = sharedPreferences, appTheme = appTheme)
                }



                if (activeSlot != null && nextSlot != null && activeSlot.second.classroom.isNotEmpty() && nextSlot.second.classroom.isNotEmpty() && activeSlot.second.classroom.take(3) != nextSlot.second.classroom.take(3)) {
                    item {
                        TransitAdvisorWidget(
                            activeSlotName = activeSlot.first.course.name,
                            activeClassroom = activeSlot.second.classroom,
                            nextSlotName = nextSlot.first.course.name,
                            nextClassroom = nextSlot.second.classroom,
                            appTheme = appTheme
                        )
                    }
                }

                if (recentlyFinishedSlot != null) {
                    item {
                        SentimentWallWidget(
                            courseName = recentlyFinishedSlot.first.course.name,
                            courseId = recentlyFinishedSlot.first.course.id,
                            sharedPreferences = sharedPreferences,
                            appTheme = appTheme
                        )
                    }
                }

                // Timeline Header
                item {
                    Text(
                        text = stringResource(R.string.home_today_schedule),
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
                                    Text(stringResource(R.string.home_no_class_today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            hasPendingHomework = courseWithSchedules.homework.any { !it.isCompleted },
                            appTheme = appTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownCard(status: CountdownStatus, appTheme: GlowTheme) {
    val isDark = isSystemInDarkTheme()
    val cardBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    val borderStroke = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveTilt(appTheme)
            .glowOrShadow(appTheme, isFeatured = status.isActive)
            .paperTexture(appTheme),
        shape = MaterialTheme.shapes.large,
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Ambient light glow inside the card top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isDark) Color(0x1A10B981) else Color(0x2434D399),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(100.dp)
                    )
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_countdown")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 2.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseScale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulseAlpha"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer {
                                    scaleX = if (status.isActive) pulseScale else 1f
                                    scaleY = if (status.isActive) pulseScale else 1f
                                    alpha = if (status.isActive) pulseAlpha else 1f
                                }
                                .background(
                                    color = if (status.isActive) Color(0xFF10B981) else if (status.courseName != null) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (status.isActive) Color(0xFF10B981) else if (status.courseName != null) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }

                    val statusInClass = stringResource(R.string.home_status_in_class)
                    val statusUpcoming = stringResource(R.string.home_status_upcoming)
                    val statusDone = stringResource(R.string.home_status_done)

                    Text(
                        text = if (status.isActive) statusInClass else if (status.courseName != null) statusUpcoming else statusDone,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (status.isActive) {
                            if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                        } else if (status.courseName != null) {
                            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status.courseName != null) {
                    Text(
                        text = status.courseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = status.classroom,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "${status.startTime} - ${status.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    GlowDivider(
                        appTheme = appTheme,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    Text(
                        text = status.countdownText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.home_no_more_classes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.home_all_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    hasPendingHomework: Boolean = false,
    appTheme: GlowTheme
) {
    val cardColor = Color(android.graphics.Color.parseColor(colorHex))
    val textColor = getContrastingTextColor(colorHex, isSystemInDarkTheme())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.6f else 1.0f),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time indicator
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (isPast) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                color = if (isPast) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }

        // Timeline Node
        Box(
            modifier = Modifier
                .width(16.dp)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_timeline")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseScale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseAlpha"
            )

            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(Color(0xFF10B981), shape = RoundedCornerShape(50))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), shape = RoundedCornerShape(50))
                )
            } else if (isPast) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(50))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(50))
                        .background(Color.Transparent, shape = RoundedCornerShape(50))
                )
            }
        }

        // Course Card
        Card(
            modifier = Modifier
                .weight(1f)
                .glowOrShadow(appTheme, isFeatured = isActive)
                .paperTexture(appTheme),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = MaterialTheme.shapes.medium,
            border = if (isActive) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                when (appTheme) {
                    GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    else -> null
                }
            }
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isPast) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPast) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(textColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_completed),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    } else if (hasPendingHomework) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_pending_todo),
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
                    text = stringResource(R.string.home_period_range, startPeriod, endPeriod),
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
    timeMinutes: Int,
    context: android.content.Context
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
            countdownText = context.getString(R.string.home_countdown_active, diff)
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
            countdownText = context.getString(R.string.home_countdown_upcoming, diff)
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

@Composable
fun DndWidget(sharedPreferences: android.content.SharedPreferences, appTheme: GlowTheme) {
    var overrideEndTime by remember { mutableLongStateOf(sharedPreferences.getLong("dnd_override_end_time", 0)) }
    val now = System.currentTimeMillis()
    val isOverridden = overrideEndTime > now
    val remainingMins = if (isOverridden) ((overrideEndTime - now) / (60 * 1000)).toInt() else 0

    LaunchedEffect(isOverridden) {
        if (isOverridden) {
            while (System.currentTimeMillis() < overrideEndTime) {
                kotlinx.coroutines.delay(10000)
                overrideEndTime = sharedPreferences.getLong("dnd_override_end_time", 0)
            }
        }
    }

    val cardBorder = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveTilt(appTheme)
            .glowOrShadow(appTheme, isFeatured = isOverridden)
            .paperTexture(appTheme),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(stringResource(R.string.home_dnd_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isOverridden) stringResource(R.string.home_dnd_extending, remainingMins) else stringResource(R.string.home_dnd_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    val current = if (overrideEndTime > System.currentTimeMillis()) overrideEndTime else System.currentTimeMillis()
                    val newTime = current + 60 * 60 * 1000
                    sharedPreferences.edit().putLong("dnd_override_end_time", newTime).apply()
                    overrideEndTime = newTime
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.home_extend_one_hour), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransitAdvisorWidget(
    activeSlotName: String,
    activeClassroom: String,
    nextSlotName: String,
    nextClassroom: String,
    appTheme: GlowTheme
) {
    val cardBorder = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, Color(0xFFFBBF24))
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, Color(0x33FBBF24))
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, Color(0x55FBBF24))
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, Color(0x33FBBF24))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveTilt(appTheme)
            .glowOrShadow(appTheme, isFeatured = true, glowColor = Color(0xFFFBBF24))
            .paperTexture(appTheme),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0x22FBBF24)),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🐈", fontSize = 28.sp)
            Column {
                Text(
                    text = stringResource(R.string.home_transit_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_transit_message, nextSlotName, nextClassroom, activeClassroom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SentimentWallWidget(
    courseName: String,
    courseId: Int,
    sharedPreferences: android.content.SharedPreferences,
    appTheme: GlowTheme
) {
    val key = "sentiment_${courseId}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
    var selectedEmoji by remember { mutableStateOf(sharedPreferences.getString(key, null)) }

    val cardBorder = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val awesomeStr = stringResource(R.string.home_sentiment_awesome)
    val nailedItStr = stringResource(R.string.home_sentiment_nailed_it)
    val mindBlownStr = stringResource(R.string.home_sentiment_mind_blown)
    val runStr = stringResource(R.string.home_sentiment_run)
    val sleepyStr = stringResource(R.string.home_sentiment_sleepy)

    if (selectedEmoji == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .interactiveTilt(appTheme)
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_sentiment_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.home_sentiment_question, courseName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf(
                        "🥳" to awesomeStr,
                        "😎" to nailedItStr,
                        "🤯" to mindBlownStr,
                        "😴" to runStr,
                        "💤" to sleepyStr
                    )
                    emojis.forEach { (emoji, label) ->
                        Column(
                            modifier = Modifier
                                .clickable {
                                    sharedPreferences.edit().putString(key, emoji).apply()
                                    selectedEmoji = emoji
                                }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .interactiveTilt(appTheme)
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
            border = BorderStroke(if (appTheme == GlowTheme.VINTAGE_LIBRARY) 0.5.dp else 1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🐈", fontSize = 24.sp)
                Column {
                    Text(stringResource(R.string.home_sentiment_synced), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.home_sentiment_report, selectedEmoji!!), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
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

package com.example.schday.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.schday.R
import com.example.schday.theme.GlowTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlowDatePickerDialog(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit,
    appTheme: GlowTheme
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(Date(initialDateMillis)) }
    var yearMonthCal by remember {
        mutableStateOf(Calendar.getInstance(Locale.getDefault()).apply { time = selectedDate })
    }

    // Resolve strings
    val prevMonthDesc = stringResource(R.string.datepicker_prev_month)
    val nextMonthDesc = stringResource(R.string.datepicker_next_month)
    val confirmStr = stringResource(R.string.confirm)
    val cancelStr = stringResource(R.string.cancel)
    val monthFormat = stringResource(R.string.datepicker_month_format)
    val dayHeaders = context.resources.getStringArray(R.array.calendar_day_headers).toList()

    Dialog(onDismissRequest = onDismissRequest) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outline

        // Setup Card shape and border based on theme
        val cardShape = when (appTheme) {
            GlowTheme.ACADEMIC_SERENITY -> MaterialTheme.shapes.large // 24dp
            GlowTheme.DEEP_CHARCOAL -> MaterialTheme.shapes.large // 12dp
            GlowTheme.AMOLED_POP -> RoundedCornerShape(12.dp)
            GlowTheme.VINTAGE_LIBRARY -> MaterialTheme.shapes.medium // 4dp
        }

        val cardBorder = when (appTheme) {
            GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, outlineColor.copy(alpha = 0.5f))
            GlowTheme.AMOLED_POP -> BorderStroke(1.5.dp, primaryColor) // Neon Green outline
            GlowTheme.VINTAGE_LIBRARY -> BorderStroke(1.dp, outlineColor)
        }

        val cardBackground = when (appTheme) {
            GlowTheme.ACADEMIC_SERENITY -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            GlowTheme.DEEP_CHARCOAL -> MaterialTheme.colorScheme.surface
            GlowTheme.AMOLED_POP -> Color.Black
            GlowTheme.VINTAGE_LIBRARY -> MaterialTheme.colorScheme.surface
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = cardShape,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                // Header details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = (yearMonthCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                            yearMonthCal = newCal
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = prevMonthDesc,
                                tint = primaryColor
                            )
                        }

                        val monthStr = SimpleDateFormat(monthFormat, Locale.getDefault()).format(yearMonthCal.time)

                        Text(
                            text = if (appTheme == GlowTheme.AMOLED_POP) monthStr.uppercase() else monthStr,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (appTheme == GlowTheme.AMOLED_POP) 2.sp else 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(onClick = {
                            val newCal = (yearMonthCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                            yearMonthCal = newCal
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = nextMonthDesc,
                                tint = primaryColor
                            )
                        }
                    }

                    // Divider styling depending on theme
                    when (appTheme) {
                        GlowTheme.AMOLED_POP -> {
                            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                val dotWidth = 3.dp.toPx()
                                val spaceWidth = 3.dp.toPx()
                                var x = 0f
                                while (x < size.width) {
                                    drawRect(
                                        color = primaryColor.copy(alpha = 0.5f),
                                        topLeft = Offset(x, 0f),
                                        size = androidx.compose.ui.geometry.Size(dotWidth, 1.dp.toPx())
                                    )
                                    x += dotWidth + spaceWidth
                                }
                            }
                        }
                        GlowTheme.VINTAGE_LIBRARY -> {
                            // Double vintage rules
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        }
                        else -> {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )
                        }
                    }
                }

                // Weekday names
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayHeaders.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Grid calculations
                val days = remember(yearMonthCal) {
                    val tempCal = (yearMonthCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                    val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val list = mutableListOf<Int?>()
                    for (i in 1 until firstDayOfWeek) {
                        list.add(null)
                    }
                    for (i in 1..maxDay) {
                        list.add(i)
                    }
                    list
                }

                val selectedCal = Calendar.getInstance().apply { time = selectedDate }
                val rows = days.chunked(7)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDays.forEach { dayNum ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNum != null) {
                                        val isCurrentSelected = selectedCal.get(Calendar.YEAR) == yearMonthCal.get(Calendar.YEAR) &&
                                                selectedCal.get(Calendar.MONTH) == yearMonthCal.get(Calendar.MONTH) &&
                                                selectedCal.get(Calendar.DAY_OF_MONTH) == dayNum

                                        val stampScale by animateFloatAsState(
                                            targetValue = if (isCurrentSelected) 1.0f else 1.3f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "picker_stamp_scale"
                                        )
                                        val stampAlpha by animateFloatAsState(
                                            targetValue = if (isCurrentSelected) 1.0f else 0.0f,
                                            animationSpec = tween(durationMillis = 180),
                                            label = "picker_stamp_alpha"
                                        )
                                        val stampRotation by animateFloatAsState(
                                            targetValue = if (isCurrentSelected) 0f else -12f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "picker_stamp_rotation"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    val newSel = (yearMonthCal.clone() as Calendar).apply {
                                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                                    }
                                                    selectedDate = newSel.time
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrentSelected || stampAlpha > 0.01f) {
                                                // Dynamic Theme Selection Indicator
                                                Canvas(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer {
                                                            scaleX = stampScale
                                                            scaleY = stampScale
                                                            rotationZ = stampRotation
                                                            alpha = stampAlpha
                                                        }
                                                ) {
                                                    when (appTheme) {
                                                        GlowTheme.AMOLED_POP -> {
                                                            // Cyberpunk Target Corners HUD
                                                            val len = 6.dp.toPx()
                                                            val w = 1.5.dp.toPx()
                                                            val secondaryNeon = Color(0xFFFF007F) // Neon Pink

                                                            // Top Left
                                                            drawLine(color = secondaryNeon, start = Offset(0f, 0f), end = Offset(len, 0f), strokeWidth = w)
                                                            drawLine(color = secondaryNeon, start = Offset(0f, 0f), end = Offset(0f, len), strokeWidth = w)
                                                            // Top Right
                                                            drawLine(color = secondaryNeon, start = Offset(size.width, 0f), end = Offset(size.width - len, 0f), strokeWidth = w)
                                                            drawLine(color = secondaryNeon, start = Offset(size.width, 0f), end = Offset(size.width, len), strokeWidth = w)
                                                            // Bottom Left
                                                            drawLine(color = secondaryNeon, start = Offset(0f, size.height), end = Offset(len, size.height), strokeWidth = w)
                                                            drawLine(color = secondaryNeon, start = Offset(0f, size.height), end = Offset(0f, size.height - len), strokeWidth = w)
                                                            // Bottom Right
                                                            drawLine(color = secondaryNeon, start = Offset(size.width, size.height), end = Offset(size.width - len, size.height), strokeWidth = w)
                                                            drawLine(color = secondaryNeon, start = Offset(size.width, size.height), end = Offset(size.width, size.height - len), strokeWidth = w)
                                                        }
                                                        GlowTheme.VINTAGE_LIBRARY -> {
                                                            // Distressed Ink Stamp concentric circles
                                                            drawCircle(
                                                                color = primaryColor.copy(alpha = 0.12f),
                                                                radius = size.minDimension / 2f
                                                            )
                                                            drawCircle(
                                                                color = primaryColor.copy(alpha = 0.8f),
                                                                radius = size.minDimension / 2f - 2.dp.toPx(),
                                                                style = Stroke(width = 1.6.dp.toPx())
                                                            )
                                                            drawCircle(
                                                                color = primaryColor.copy(alpha = 0.6f),
                                                                radius = size.minDimension / 2f - 5.dp.toPx(),
                                                                style = Stroke(width = 0.8.dp.toPx())
                                                            )

                                                            // splatters
                                                            val center = size.minDimension / 2f
                                                            val splatters = listOf(
                                                                Pair(-0.4f, -0.4f),
                                                                Pair(0.4f, -0.35f),
                                                                Pair(-0.35f, 0.45f),
                                                                Pair(0.35f, 0.4f)
                                                            )
                                                            splatters.forEach { (dx, dy) ->
                                                                drawCircle(
                                                                    color = primaryColor.copy(alpha = 0.45f),
                                                                    radius = 0.7.dp.toPx(),
                                                                    center = Offset(
                                                                        x = center + dx * center * 0.8f,
                                                                        y = center + dy * center * 0.8f
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        GlowTheme.DEEP_CHARCOAL -> {
                                                            // Rounded square container
                                                            drawRoundRect(
                                                                color = primaryColor,
                                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                                            )
                                                        }
                                                        GlowTheme.ACADEMIC_SERENITY -> {
                                                            // Gentle smoke glass circle
                                                            drawCircle(
                                                                color = primaryColor.copy(alpha = 0.25f),
                                                                radius = size.minDimension / 2f
                                                            )
                                                            drawCircle(
                                                                color = primaryColor.copy(alpha = 0.6f),
                                                                radius = size.minDimension / 2f,
                                                                style = Stroke(width = 1.5.dp.toPx())
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Text color adjustments based on active selected states
                                            val dayColor = when {
                                                isCurrentSelected -> {
                                                    when (appTheme) {
                                                        GlowTheme.DEEP_CHARCOAL -> MaterialTheme.colorScheme.onPrimary
                                                        GlowTheme.AMOLED_POP -> Color(0xFFFF007F) // Neon Pink for select
                                                        else -> primaryColor
                                                    }
                                                }
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }

                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = dayColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(
                            text = cancelStr,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (appTheme == GlowTheme.AMOLED_POP) FontFamily.Monospace else null
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Confirm button with theme specific shapes & borders
                    val confirmButtonShape = when (appTheme) {
                        GlowTheme.ACADEMIC_SERENITY -> MaterialTheme.shapes.medium
                        GlowTheme.DEEP_CHARCOAL -> MaterialTheme.shapes.small
                        GlowTheme.AMOLED_POP -> RoundedCornerShape(4.dp)
                        GlowTheme.VINTAGE_LIBRARY -> MaterialTheme.shapes.small
                    }

                    val confirmButtonBorder = when (appTheme) {
                        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, primaryColor)
                        else -> null
                    }

                    val confirmButtonColors = when (appTheme) {
                        GlowTheme.AMOLED_POP -> ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = primaryColor
                        )
                        GlowTheme.VINTAGE_LIBRARY -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        else -> ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Surface(
                        shape = confirmButtonShape,
                        border = confirmButtonBorder,
                        color = Color.Transparent
                    ) {
                        Button(
                            onClick = {
                                onDateSelected(selectedDate.time)
                            },
                            colors = confirmButtonColors,
                            shape = confirmButtonShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = confirmStr,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (appTheme == GlowTheme.AMOLED_POP) FontFamily.Monospace else null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

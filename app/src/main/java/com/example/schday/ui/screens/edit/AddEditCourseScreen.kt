package com.example.schday.ui.screens.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.theme.MorandiColors
import com.example.schday.theme.getContrastingTextColor
import com.example.schday.utils.DateUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class SlotInput(
    val id: Int = 0,
    val dayOfWeek: Int = 1,
    val startPeriod: Int = 1,
    val endPeriod: Int = 2,
    val classroom: String = "",
    val activeWeeks: Set<Int> = (1..20).toSet()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseScreen(
    repository: DataRepository,
    courseId: Int?,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentSemester by repository.getCurrentSemester().collectAsStateWithLifecycle(initialValue = null)

    // Load course if editing
    val courseFlow = remember(courseId) {
        if (courseId != null) repository.getCourseById(courseId)
        else flowOf(null)
    }
    val courseData by courseFlow.collectAsStateWithLifecycle(initialValue = null)

    // Form fields
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(MorandiColors.first()) }
    var slots by remember { mutableStateOf(listOf(SlotInput())) }

    // Flag to ensure we only load data once from database
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(courseData) {
        val data = courseData
        if (data != null && !isLoaded) {
            name = data.course.name
            teacher = data.course.teacher
            colorHex = data.course.colorHex
            slots = data.slots.map { slot ->
                SlotInput(
                    id = slot.id,
                    dayOfWeek = slot.dayOfWeek,
                    startPeriod = slot.startPeriod,
                    endPeriod = slot.endPeriod,
                    classroom = slot.classroom,
                    activeWeeks = DateUtils.parseActiveWeeks(slot.activeWeeks).toSet()
                )
            }
            if (slots.isEmpty()) {
                slots = listOf(SlotInput())
            }
            isLoaded = true
        }
    }

    // Dialog state for week selector
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (courseId == null) "添加课程" else "编辑课程", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isBlank()) {
                                return@IconButton
                            }
                            val semId = currentSemester?.id ?: return@IconButton
                            coroutineScope.launch {
                                val course = Course(
                                    id = courseId ?: 0,
                                    semesterId = semId,
                                    name = name,
                                    teacher = teacher,
                                    colorHex = colorHex
                                )
                                val dbSlots = slots.map { s ->
                                    ScheduleSlot(
                                        id = s.id,
                                        courseId = courseId ?: 0,
                                        dayOfWeek = s.dayOfWeek,
                                        startPeriod = s.startPeriod,
                                        endPeriod = s.endPeriod,
                                        classroom = s.classroom,
                                        activeWeeks = s.activeWeeks.sorted().joinToString(",")
                                    )
                                }
                                repository.saveCourseWithSlots(course, dbSlots)
                                onBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Basic Info
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("基本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("课程名称 *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("任课老师") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. Color Selection
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("课程卡片色彩", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorandiColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { colorHex = hex }
                                    .let {
                                        if (colorHex == hex) {
                                            it.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), CircleShape)
                                        } else {
                                            it
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorHex == hex) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = getContrastingTextColor(hex, isSystemInDarkTheme()),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Time Slots Builder
            Text("上课时间与教室", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            slots.forEachIndexed { index, slot ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("时段 #${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            
                            if (slots.size > 1) {
                                IconButton(onClick = { slots = slots.filterIndexed { i, _ -> i != index } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除该时段", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Day of Week selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..7).forEach { day ->
                                val isSelected = slot.dayOfWeek == day
                                Button(
                                    onClick = { 
                                        slots = slots.mapIndexed { i, s -> 
                                            if (i == index) s.copy(dayOfWeek = day) else s 
                                        } 
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(DateUtils.getDayName(day), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Period Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start Period
                            Column(modifier = Modifier.weight(1f)) {
                                Text("开始节次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("第 ${slot.startPeriod} 节")
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (1..12).forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("第 $p 节") },
                                                onClick = {
                                                    slots = slots.mapIndexed { i, s ->
                                                        if (i == index) {
                                                            val end = if (s.endPeriod < p) p else s.endPeriod
                                                            s.copy(startPeriod = p, endPeriod = end)
                                                        } else s
                                                    }
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // End Period
                            Column(modifier = Modifier.weight(1f)) {
                                Text("结束节次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("第 ${slot.endPeriod} 节")
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (slot.startPeriod..12).forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("第 $p 节") },
                                                onClick = {
                                                    slots = slots.mapIndexed { i, s ->
                                                        if (i == index) s.copy(endPeriod = p) else s
                                                    }
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Classroom TextField
                        OutlinedTextField(
                            value = slot.classroom,
                            onValueChange = { text ->
                                slots = slots.mapIndexed { i, s ->
                                    if (i == index) s.copy(classroom = text) else s
                                }
                            },
                            label = { Text("上课教室") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Active Weeks trigger
                        OutlinedButton(
                            onClick = { editingSlotIndex = index },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val activeWeeksCount = slot.activeWeeks.size
                            Text(
                                if (activeWeeksCount == 20) "上课周：全选 (1-20周)"
                                else "上课周：已选 $activeWeeksCount 周 (点按编辑)"
                            )
                        }
                    }
                }
            }

            // Button to add another time slot
            Button(
                onClick = { slots = slots + SlotInput() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("增加上课时间段", fontWeight = FontWeight.Bold)
            }

            // Delete Course Button (if editing)
            if (courseId != null && courseData != null) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            courseData?.course?.let { repository.deleteCourse(it) }
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("删除这门课程", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Week selector Dialog
    if (editingSlotIndex != null) {
        val index = editingSlotIndex!!
        val slot = slots[index]
        var tempWeeks by remember(slot) { mutableStateOf(slot.activeWeeks) }

        AlertDialog(
            onDismissRequest = { editingSlotIndex = null },
            title = { Text("选择上课周", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Quick Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { tempWeeks = (1..20).toSet() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("全选", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { tempWeeks = (1..20).filter { it % 2 != 0 }.toSet() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("单周", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { tempWeeks = (1..20).filter { it % 2 == 0 }.toSet() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("双周", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { tempWeeks = emptySet() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("清空", fontSize = 11.sp)
                        }
                    }

                    // 4x5 Checkbox grid for weeks 1..20
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 1..4) {
                                    val week = row * 4 + col
                                    val isChecked = tempWeeks.contains(week)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                tempWeeks = if (isChecked) tempWeeks - week else tempWeeks + week
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "$week 周",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        slots = slots.mapIndexed { i, s ->
                            if (i == index) s.copy(activeWeeks = tempWeeks) else s
                        }
                        editingSlotIndex = null
                    }
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlotIndex = null }) {
                    Text("取消")
                }
            }
        )
    }
}

package com.example.schday.ui.screens.import

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.parser.ExcelParser
import com.example.schday.theme.GlowTheme
import com.example.schday.theme.MorandiColors
import com.example.schday.theme.paperTexture
import com.example.schday.ui.components.GlowDialog
import com.example.schday.utils.DateUtils
import com.example.schday.utils.ScheduleClashDetector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ImportConflict(
    val newCourse: Course,
    val newSlots: List<ScheduleSlot>,
    val existingCourse: Course,
    val existingSlots: List<ScheduleSlot>,
    val clashingSlots: List<Pair<ScheduleSlot, ScheduleSlot>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCoursesScreen(
    repository: DataRepository,
    appTheme: GlowTheme,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentSemester by repository.getCurrentSemester().collectAsStateWithLifecycle(initialValue = null)

    var selectedTab by remember { mutableStateOf(0) } // 0 = AI Screenshot, 1 = CSV
    val pendingConflicts = remember { mutableStateListOf<ImportConflict>() }

    // Helper to check for schedule clashes and process import
    val processImport: (List<Pair<Course, List<ScheduleSlot>>>) -> Unit = { parsedList ->
        coroutineScope.launch {
            try {
                if (currentSemester == null) return@launch
                val existingCourses = repository.getCoursesBySemester(currentSemester!!.id).first()
                
                val conflicts = mutableListOf<ImportConflict>()
                val safeImports = mutableListOf<Pair<Course, List<ScheduleSlot>>>()

                for ((newCourse, newSlots) in parsedList) {
                    val clashes = mutableListOf<Pair<ScheduleSlot, ScheduleSlot>>()
                    var conflictingCourse: Course? = null
                    var conflictingSlots = listOf<ScheduleSlot>()
                    
                    for (existing in existingCourses) {
                        val slotClashes = ScheduleClashDetector.findClashes(newSlots, existing.slots)
                        if (slotClashes.isNotEmpty()) {
                            clashes.addAll(slotClashes)
                            conflictingCourse = existing.course
                            conflictingSlots = existing.slots
                            break // Found a clash, handle it
                        }
                    }
                    
                    if (clashes.isNotEmpty() && conflictingCourse != null) {
                        conflicts.add(
                            ImportConflict(
                                newCourse = newCourse,
                                newSlots = newSlots,
                                existingCourse = conflictingCourse,
                                existingSlots = conflictingSlots,
                                clashingSlots = clashes
                            )
                        )
                    } else {
                        safeImports.add(Pair(newCourse, newSlots))
                    }
                }
                
                // Save safe ones immediately
                safeImports.forEach { (course, slots) ->
                    repository.saveCourseWithSlots(course, slots)
                }
                
                if (conflicts.isNotEmpty()) {
                    pendingConflicts.addAll(conflicts)
                } else {
                    Toast.makeText(context, "成功导入 ${parsedList.size} 门课程！", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "解析或检查冲突失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Excel/CSV import contract launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentSemester != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val parsed = ExcelParser.parseCsv(inputStream, currentSemester!!.id)
                        processImport(parsed)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var jsonText by remember { mutableStateOf("") }

    // Dialog showing clashes
    if (pendingConflicts.isNotEmpty()) {
        val currentConflict = pendingConflicts.first()
        GlowDialog(
            onDismissRequest = {
                pendingConflicts.clear()
                onBack()
            },
            title = {
                Text(
                    text = "课程日程冲突",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Keep Both
                            repository.saveCourseWithSlots(currentConflict.newCourse, currentConflict.newSlots)
                            pendingConflicts.removeAt(0)
                            if (pendingConflicts.isEmpty()) {
                                Toast.makeText(context, "导入完成！", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("保留两者", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            // Overwrite Existing (Delete conflicting course and insert new one)
                            repository.deleteCourse(currentConflict.existingCourse)
                            repository.saveCourseWithSlots(currentConflict.newCourse, currentConflict.newSlots)
                            pendingConflicts.removeAt(0)
                            if (pendingConflicts.isEmpty()) {
                                Toast.makeText(context, "导入完成！", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text("覆盖已有", fontWeight = FontWeight.Bold)
                }
            },
            neutralButton = {
                TextButton(
                    onClick = {
                        pendingConflicts.removeAt(0)
                        if (pendingConflicts.isEmpty()) {
                            Toast.makeText(context, "导入完成！", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ) {
                    Text("跳过此门")
                }
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "您正在导入的课程与已有课程存在时间重叠：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clash slot info
                val clash = currentConflict.clashingSlots.firstOrNull()
                if (clash != null) {
                    val (newSlot, existingSlot) = clash
                    val dayName = DateUtils.getDayName(newSlot.dayOfWeek)
                    val overlapWeeks = DateUtils.parseActiveWeeks(newSlot.activeWeeks)
                        .intersect(DateUtils.parseActiveWeeks(existingSlot.activeWeeks).toSet())
                        .joinToString(", ")
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "冲突时间：$dayName 第 ${newSlot.startPeriod}-${newSlot.endPeriod} 节",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "重叠周数：第 $overlapWeeks 周",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Existing Course Card (Academic Serenity Slate Blue / Muted style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "已有课程: ${currentConflict.existingCourse.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentConflict.existingCourse.teacher.isNotBlank()) {
                            Text(
                                text = "教师: ${currentConflict.existingCourse.teacher}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val existingSlot = currentConflict.existingSlots.firstOrNull()
                        if (existingSlot != null) {
                            Text(
                                text = "教室: ${existingSlot.classroom}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // New Course Card (Academic Serenity Dusty Pink / Warning style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "待导入课程: ${currentConflict.newCourse.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentConflict.newCourse.teacher.isNotBlank()) {
                            Text(
                                text = "教师: ${currentConflict.newCourse.teacher}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val newSlot = currentConflict.newSlots.firstOrNull()
                        if (newSlot != null) {
                            Text(
                                text = "教室: ${newSlot.classroom}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课程表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selection Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("AI截图", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CSV表格", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            when (selectedTab) {
                0 -> {
                    AiScreenshotImportPanel(
                        hasSemester = currentSemester != null,
                        jsonText = jsonText,
                        onJsonChange = { jsonText = it },
                        appTheme = appTheme,
                        onImport = {
                            if (currentSemester == null) return@AiScreenshotImportPanel
                            coroutineScope.launch {
                                try {
                                    var sanitizedJson = jsonText.trim()
                                    if (sanitizedJson.startsWith("```")) {
                                        sanitizedJson = sanitizedJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                                    }
                                    val array = org.json.JSONArray(sanitizedJson)
                                    val parsedList = mutableListOf<Pair<Course, List<ScheduleSlot>>>()
                                    var colorIdx = 0
                                    for (i in 0 until array.length()) {
                                        val obj = array.getJSONObject(i)
                                        val course = Course(
                                            semesterId = currentSemester!!.id,
                                            name = obj.getString("name"),
                                            teacher = obj.optString("teacher", ""),
                                            colorHex = MorandiColors[colorIdx % MorandiColors.size]
                                        )
                                        colorIdx++

                                        val slot = ScheduleSlot(
                                            courseId = 0,
                                            dayOfWeek = obj.getInt("day"),
                                            startPeriod = obj.getInt("start"),
                                            endPeriod = obj.getInt("end"),
                                            classroom = obj.optString("classroom", ""),
                                            activeWeeks = obj.optString("weeks", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                                        )
                                        parsedList.add(Pair(course, listOf(slot)))
                                    }
                                    processImport(parsedList)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "解析 AI 数据失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
                1 -> {
                    CsvImportPanel(
                        hasSemester = currentSemester != null,
                        appTheme = appTheme,
                        onPickFile = { filePickerLauncher.launch("text/comma-separated-values") }
                    )
                }
            }
        }
    }
}

@Composable
fun CsvImportPanel(hasSemester: Boolean, appTheme: GlowTheme, onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📊 CSV 课程表导入", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "您可以使用 Excel 创建课表，保存为 CSV 格式后导入。模板格式要求如下：\n" +
                    "每行代表一个上课时段，逗号分隔，列顺序为：\n" +
                    "课程名称, 任课教师, 上课教室, 星期几 (1-7), 开始节次 (1-12), 结束节次 (1-12), 逗号分隔的周数...\n" +
                    "例: 高等数学, 张教授, 教三302, 1, 1, 2, 1,2,3,4,5,6,7,8",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPickFile,
            enabled = hasSemester,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("选择 CSV 文件导入")
        }
        if (!hasSemester) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("请先去设置中创建当前活动的学期", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AiScreenshotImportPanel(
    hasSemester: Boolean,
    jsonText: String,
    onJsonChange: (String) -> Unit,
    appTheme: GlowTheme,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    val promptText = """
        你是一个专业的课表数据提取 AI。请仔细分析我上传的课表截图，把其中的课程名称、老师、教室、上课星期（1-7，1代表周一，7代表周日）、开始/结束节次（1-12）、以及上课周数等提取成标准的 JSON 数组格式并直接输出（不要带多余的解释，只需要 JSON 格式）。格式示例如下：
        [
          {
            "name": "高等数学",
            "teacher": "李教授",
            "classroom": "教三302",
            "day": 1,
            "start": 3,
            "end": 4,
            "weeks": "1,2,3,4,5,6,7,8"
          }
        ]
    """.trimIndent()

    val borderStroke = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("📷 AI 截图智能提取", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                text = "您可以对任何其他软件或网页上的课表进行截图。复制下方精心定制 of AI 提示词，并连同截图一起发送给多模态 AI（如 ChatGPT、Claude、Gemini），AI 识别后会将课表转换为 JSON 格式，您将其粘贴在下方即可快速导入！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .paperTexture(appTheme),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = MaterialTheme.shapes.medium,
                border = borderStroke
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡 多模态 AI 提示词 (可直接复制):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("AI Prompt", promptText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "提示词已复制到剪贴板，请去发送给多模态 AI 吧！", Toast.LENGTH_SHORT).show()
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("复制 AI 提示词", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = jsonText,
                onValueChange = onJsonChange,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("粘贴 AI 输出的 JSON 代码块") },
                placeholder = { Text("[\n  {\n    \"name\": \"高等数学\",\n    ...\n  }\n]") },
                shape = MaterialTheme.shapes.medium
            )
        }
        item {
            Button(
                onClick = onImport,
                enabled = hasSemester && jsonText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("开始解析并一键导入", fontWeight = FontWeight.Bold)
            }
            if (!hasSemester) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("请先去设置中创建当前活动的学期", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

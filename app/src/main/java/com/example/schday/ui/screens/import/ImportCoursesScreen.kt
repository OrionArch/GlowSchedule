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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.R
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

    // Pre-resolve strings for Toast/callback usage
    val importCompleteStr = stringResource(R.string.import_complete)

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = AI Screenshot, 1 = CSV
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
                    Toast.makeText(context, context.getString(R.string.import_success, parsedList.size), Toast.LENGTH_SHORT).show()
                    onBack()
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.import_parse_or_clash_fail, e.message), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, context.getString(R.string.import_fail, e.message), Toast.LENGTH_LONG).show()
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
                    text = stringResource(R.string.import_clash_title),
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
                                Toast.makeText(context, importCompleteStr, Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(R.string.import_keep_both), fontWeight = FontWeight.Bold)
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
                                Toast.makeText(context, importCompleteStr, Toast.LENGTH_SHORT).show()
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
                    Text(stringResource(R.string.import_overwrite), fontWeight = FontWeight.Bold)
                }
            },
            neutralButton = {
                TextButton(
                    onClick = {
                        pendingConflicts.removeAt(0)
                        if (pendingConflicts.isEmpty()) {
                            Toast.makeText(context, importCompleteStr, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.import_skip))
                }
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_clash_overlap_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clash slot info
                val clash = currentConflict.clashingSlots.firstOrNull()
                if (clash != null) {
                    val (newSlot, existingSlot) = clash
                    val dayName = DateUtils.getDayName(context, newSlot.dayOfWeek)
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
                                text = stringResource(R.string.import_clash_time, dayName, newSlot.startPeriod, newSlot.endPeriod),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.import_clash_weeks, overlapWeeks),
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
                            text = stringResource(R.string.import_existing_course, currentConflict.existingCourse.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentConflict.existingCourse.teacher.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.import_teacher_label, currentConflict.existingCourse.teacher),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val existingSlot = currentConflict.existingSlots.firstOrNull()
                        if (existingSlot != null) {
                            Text(
                                text = stringResource(R.string.import_classroom_label, existingSlot.classroom),
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
                            text = stringResource(R.string.import_new_course, currentConflict.newCourse.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentConflict.newCourse.teacher.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.import_teacher_label, currentConflict.newCourse.teacher),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val newSlot = currentConflict.newSlots.firstOrNull()
                        if (newSlot != null) {
                            Text(
                                text = stringResource(R.string.import_classroom_label, newSlot.classroom),
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
                title = { Text(stringResource(R.string.import_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.import_tab_ai), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.import_tab_csv), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
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
                                    Toast.makeText(context, context.getString(R.string.import_ai_parse_fail, e.message), Toast.LENGTH_LONG).show()
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
        Text(stringResource(R.string.import_csv_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.import_csv_instructions),
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
            Text(stringResource(R.string.import_csv_pick))
        }
        if (!hasSemester) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.create_semester_settings_hint), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
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
    val promptText = stringResource(R.string.import_ai_prompt_text)

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
            Text(stringResource(R.string.import_ai_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                text = stringResource(R.string.import_ai_description),
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
                    Text(stringResource(R.string.import_ai_prompt_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            Toast.makeText(context, context.getString(R.string.import_ai_copied), Toast.LENGTH_SHORT).show()
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.import_copy_prompt), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = jsonText,
                onValueChange = onJsonChange,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text(stringResource(R.string.import_json_label)) },
                placeholder = { Text(stringResource(R.string.import_json_placeholder)) },
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
                Text(stringResource(R.string.import_start_parse), fontWeight = FontWeight.Bold)
            }
            if (!hasSemester) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.create_semester_settings_hint), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

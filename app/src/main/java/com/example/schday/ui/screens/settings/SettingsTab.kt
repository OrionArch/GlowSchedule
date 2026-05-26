package com.example.schday.ui.screens.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.R
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.Semester
import com.example.schday.theme.GlowTheme
import com.example.schday.theme.glowOrShadow
import com.example.schday.theme.paperTexture
import com.example.schday.theme.GlowDivider
import com.example.schday.utils.DateUtils
import com.example.schday.ui.components.GlowDatePickerDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    semesters: List<Semester>,
    currentSemester: Semester?,
    periods: List<PeriodTime>,
    appTheme: GlowTheme,
    onThemeChange: (GlowTheme) -> Unit,
    onSelectSemester: (Int) -> Unit,
    onAddSemester: (Semester) -> Unit,
    onUpdatePeriods: (List<PeriodTime>) -> Unit,
    onImportJson: (String) -> Unit,
    onExportJson: suspend () -> String,
    onImportClick: () -> Unit, // Navigate to crawler import screen
    onClearData: () -> Unit,
    onLoadDemoData: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog state for adding a semester
    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var newSemesterName by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    var newSemesterStartMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Dialog state for editing class period times
    var showEditPeriodsDialog by remember { mutableStateOf(false) }

    // Active time editing states (for period row and time field index)
    var activeTimeEditPeriodIdx by remember { mutableStateOf<Int?>(null) }
    var activeTimeEditIsStart by remember { mutableStateOf(true) }

    // Silent Mode Settings
    val sharedPreferences = remember { context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE) }
    var autoMuteEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_mute_enabled", false)) }
    var silentModeType by remember { mutableIntStateOf(sharedPreferences.getInt("auto_mute_type", 0)) } // 0 = DND, 1 = Vibrate, 2 = Silent
    var preClassReminderOffset by remember { mutableIntStateOf(sharedPreferences.getInt("pre_class_reminder_offset", 10)) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // Resolve strings for non-Composable contexts
    val restoreSuccessStr = stringResource(R.string.settings_restore_success)
    val restoreFailStr = stringResource(R.string.settings_restore_fail)
    val exportSuccessStr = stringResource(R.string.settings_export_success)
    val exportFailStr = stringResource(R.string.settings_export_fail)
    val grantDndStr = stringResource(R.string.settings_grant_dnd)
    val cannotOpenSettingsStr = stringResource(R.string.settings_cannot_open_settings)
    val dataClearedStr = stringResource(R.string.settings_data_cleared)
    val demoLoadedStr = stringResource(R.string.settings_demo_loaded)
    val muteModes = listOf(
        stringResource(R.string.settings_mute_dnd),
        stringResource(R.string.settings_mute_vibrate),
        stringResource(R.string.settings_mute_silent)
    )
    val reminderOnTimeStr = stringResource(R.string.settings_reminder_on_time)

    // JSON file picker activity launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                inputStream?.close()
                onImportJson(stringBuilder.toString())
                Toast.makeText(context, restoreSuccessStr, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, restoreFailStr.format(e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    // JSON export file saver activity launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, exportSuccessStr, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, exportFailStr.format(e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val cardBorder = when (appTheme) {
        GlowTheme.AMOLED_POP -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.DEEP_CHARCOAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.VINTAGE_LIBRARY -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        GlowTheme.ACADEMIC_SERENITY -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 0. Course Table Import Section (Portal & Excel)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.settings_import_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_import_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.settings_go_import), fontWeight = FontWeight.Bold)
                }
            }
        }

        // 0.5 Custom Theme Customization Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_theme_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.settings_theme_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlowTheme.entries.forEach { theme ->
                        val isSelected = appTheme == theme
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val borderWidth = if (isSelected) 2.dp else 1.dp

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onThemeChange(theme) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(borderWidth, borderCol),
                            colors = CardDefaults.cardColors(
                                containerColor = when(theme) {
                                    GlowTheme.ACADEMIC_SERENITY -> Color(0xFFFBF9F4)
                                    GlowTheme.DEEP_CHARCOAL -> Color(0xFF121312)
                                    GlowTheme.AMOLED_POP -> Color(0xFF000000)
                                    GlowTheme.VINTAGE_LIBRARY -> Color(0xFF201A16)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = theme.getDisplayName(context),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = when(theme) {
                                        GlowTheme.ACADEMIC_SERENITY -> Color(0xFF1B1C19)
                                        else -> Color(0xFFF4F1DE)
                                    },
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (p, s, t) = when(theme) {
                                        GlowTheme.ACADEMIC_SERENITY -> Triple(Color(0xFF54624E), Color(0xFF7B5455), Color(0xFF4F6071))
                                        GlowTheme.DEEP_CHARCOAL -> Triple(Color(0xFFBCCBB2), Color(0xFFECBBBA), Color(0xFFB6C9DB))
                                        GlowTheme.AMOLED_POP -> Triple(Color(0xFF00FF66), Color(0xFFFF007F), Color(0xFF00FFFF))
                                        GlowTheme.VINTAGE_LIBRARY -> Triple(Color(0xFF8FA382), Color(0xFFD4A373), Color(0xFFA2A2D0))
                                    }
                                    Box(modifier = Modifier.size(8.dp).background(p, CircleShape))
                                    Box(modifier = Modifier.size(8.dp).background(s, CircleShape))
                                    Box(modifier = Modifier.size(8.dp).background(t, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Semester Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_semester_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                semesters.forEach { semester ->
                    val isCurrent = semester.id == currentSemester?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSemester(semester.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(semester.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(semester.startDate))
                            Text(stringResource(R.string.settings_first_day, formattedDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.settings_current_selection), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Button(
                    onClick = { showAddSemesterDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(stringResource(R.string.settings_add_semester))
                }
            }
        }

        // 2. Class Timings Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                    Text(stringResource(R.string.settings_schedule_table), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showEditPeriodsDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.settings_edit_time))
                    }
                }

                periods.take(5).forEach { period ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.settings_period_label, period.periodNumber), style = MaterialTheme.typography.bodyMedium)
                        Text("${period.startTime} - ${period.endTime}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (periods.size > 5) {
                    Text(stringResource(R.string.settings_remaining_periods), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 3. Silent Mode Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_silent_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_enable_auto_mute))
                    Switch(
                        checked = autoMuteEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                if (!notificationManager.isNotificationPolicyAccessGranted) {
                                    Toast.makeText(context, grantDndStr, Toast.LENGTH_LONG).show()
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                    return@Switch
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        showExactAlarmDialog = true
                                        return@Switch
                                    }
                                }
                            }
                            autoMuteEnabled = checked
                            sharedPreferences.edit().putBoolean("auto_mute_enabled", checked).apply()
                        }
                    )
                }

                if (autoMuteEnabled) {
                    Text(stringResource(R.string.settings_mute_mode_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        muteModes.forEachIndexed { index, mode ->
                            val isSelected = silentModeType == index
                            OutlinedButton(
                                onClick = {
                                    silentModeType = index
                                    sharedPreferences.edit().putInt("auto_mute_type", index).apply()
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(mode, fontSize = 11.sp)
                            }
                        }
                    }
                }

                GlowDivider(appTheme = appTheme, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(stringResource(R.string.settings_pre_class_reminder), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.bodyMedium)

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (preClassReminderOffset == 0) reminderOnTimeStr else stringResource(R.string.settings_reminder_minutes, preClassReminderOffset))
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(0, 5, 10, 15, 20, 30).forEach { offset ->
                                DropdownMenuItem(
                                    text = { Text(if (offset == 0) reminderOnTimeStr else stringResource(R.string.settings_reminder_minutes, offset)) },
                                    onClick = {
                                        if (offset > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                            if (!alarmManager.canScheduleExactAlarms()) {
                                                showExactAlarmDialog = true
                                            }
                                        }
                                        preClassReminderOffset = offset
                                        sharedPreferences.edit().putInt("pre_class_reminder_offset", offset).apply()
                                        expanded = false

                                        // Reactively reschedule alarms when reminder offset changes
                                        coroutineScope.launch {
                                            val repository = com.example.schday.data.DefaultDataRepository(
                                                com.example.schday.data.AppDatabase.getDatabase(context),
                                                context
                                            )
                                            com.example.schday.scheduler.AlarmScheduler.scheduleTodayAlarms(context, repository)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Data Backup Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_backup_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("schday_backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(stringResource(R.string.settings_export_backup))
                    }

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.settings_import_backup))
                    }
                }
            }
        }

        // 5. Data Reset & Debug Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glowOrShadow(appTheme, isFeatured = false)
                .paperTexture(appTheme),
            shape = MaterialTheme.shapes.large,
            border = cardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onClearData()
                            Toast.makeText(context, dataClearedStr, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_clear_data))
                    }

                    Button(
                        onClick = {
                            onLoadDemoData()
                            Toast.makeText(context, demoLoadedStr, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(stringResource(R.string.settings_load_demo))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // New Semester Dialog
    if (showAddSemesterDialog) {
        AlertDialog(
            onDismissRequest = { showAddSemesterDialog = false },
            title = { Text(stringResource(R.string.settings_new_semester), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSemesterName,
                        onValueChange = { newSemesterName = it },
                        label = { Text(stringResource(R.string.settings_semester_name)) },
                        placeholder = { Text(stringResource(R.string.settings_semester_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(newSemesterStartMillis))
                        Text(stringResource(R.string.settings_first_monday, formattedDate))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSemesterName.isBlank()) return@TextButton

                        val cal = Calendar.getInstance().apply {
                            timeInMillis = newSemesterStartMillis
                            firstDayOfWeek = Calendar.MONDAY
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val sem = Semester(
                            name = newSemesterName,
                            startDate = cal.timeInMillis,
                            totalWeeks = 20,
                            isCurrent = false
                        )
                        onAddSemester(sem)
                        newSemesterName = ""
                        showAddSemesterDialog = false
                    }
                ) {
                    Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Retro Ink-Stamped Date Picker Dialog
    if (showDatePicker) {
        GlowDatePickerDialog(
            initialDateMillis = newSemesterStartMillis,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedTime ->
                newSemesterStartMillis = selectedTime
                showDatePicker = false
            },
            appTheme = appTheme
        )
    }

    // Edit Period Times Dialog
    if (showEditPeriodsDialog) {
        var tempPeriods by remember(periods) { mutableStateOf(periods.map { it.copy() }) }

        AlertDialog(
            onDismissRequest = { showEditPeriodsDialog = false },
            title = { Text(stringResource(R.string.settings_edit_schedule), fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tempPeriods.size) { idx ->
                            val period = tempPeriods[idx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(stringResource(R.string.settings_period_label, period.periodNumber), modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                // Clickable field for Start Time picker
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeTimeEditPeriodIdx = idx
                                            activeTimeEditIsStart = true
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = period.startTime,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        placeholder = { Text("08:00") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }

                                Text("-")

                                // Clickable field for End Time picker
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeTimeEditPeriodIdx = idx
                                            activeTimeEditIsStart = false
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = period.endTime,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        placeholder = { Text("08:45") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePeriods(tempPeriods)
                        showEditPeriodsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_save_schedule), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPeriodsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )

        // Custom Time Picker Dialog
        if (activeTimeEditPeriodIdx != null) {
            val idx = activeTimeEditPeriodIdx!!
            val period = tempPeriods[idx]
            val initialTime = if (activeTimeEditIsStart) period.startTime else period.endTime
            TimeTumblerDialog(
                initialTime = if (initialTime.contains(":")) initialTime else "08:00",
                onDismiss = { activeTimeEditPeriodIdx = null },
                onConfirm = { selectedTime ->
                    tempPeriods = tempPeriods.mapIndexed { i, p ->
                        if (i == idx) {
                            if (activeTimeEditIsStart) p.copy(startTime = selectedTime)
                            else p.copy(endTime = selectedTime)
                        } else p
                    }
                    activeTimeEditPeriodIdx = null
                }
            )
        }
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.settings_exact_alarm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_exact_alarm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, cannotOpenSettingsStr, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_go_settings), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// Infinite Scroll Time Tumbler Dialog Component
@Composable
fun TimeTumblerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    // 1000 * length is a large number to simulate an infinite list
    val hourScrollState = rememberLazyListState(initialFirstVisibleItemIndex = (initialHour + 1000 * 24) - 2)
    val minuteScrollState = rememberLazyListState(initialFirstVisibleItemIndex = (initialMinute + 1000 * 60) - 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_set_class_time),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour wheel
                TumblerWheel(
                    items = hours,
                    listState = hourScrollState,
                    format = { String.format(Locale.getDefault(), "%02d", it) },
                    modifier = Modifier.weight(1f)
                )

                // Divider dots
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Minute wheel
                TumblerWheel(
                    items = minutes,
                    listState = minuteScrollState,
                    format = { String.format(Locale.getDefault(), "%02d", it) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Extract values from center index
                    val hIdx = hourScrollState.firstVisibleItemIndex + 2
                    val mIdx = minuteScrollState.firstVisibleItemIndex + 2
                    val finalHour = hours[hIdx % hours.size]
                    val finalMinute = minutes[mIdx % minutes.size]
                    onConfirm(String.format(Locale.getDefault(), "%02d:%02d", finalHour, finalMinute))
                }
            ) {
                Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun TumblerWheel(
    items: List<Int>,
    listState: LazyListState,
    format: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    // Derive selection index from center item
    val selectedIndex = remember {
        derivedStateOf {
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleInfo.isEmpty()) 0
            else {
                val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                val closest = visibleInfo.minByOrNull { Math.abs((it.offset + it.size / 2) - viewportCenter) }
                closest?.index ?: 0
            }
        }
    }

    LaunchedEffect(selectedIndex.value) {
        val idx = selectedIndex.value
        if (lastSelectedIndex != -1 && lastSelectedIndex != idx) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
        lastSelectedIndex = idx
    }

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Overlay Selection Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {}

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(1000000) { index ->
                val item = items[index % items.size]
                val isSelected = index == selectedIndex.value
                val scale = if (isSelected) 1.25f else 0.8f
                val alpha = if (isSelected) 1f else 0.4f

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = alpha
                        )
                    )
                }
            }
        }
    }
}

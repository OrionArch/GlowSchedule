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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schday.data.entity.PeriodTime
import com.example.schday.data.entity.Semester
import com.example.schday.utils.DateUtils
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
    onSelectSemester: (Int) -> Unit,
    onAddSemester: (Semester) -> Unit,
    onUpdatePeriods: (List<PeriodTime>) -> Unit,
    onImportJson: (String) -> Unit,
    onExportJson: () -> String,
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
    var newSemesterStartMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Dialog state for editing class period times
    var showEditPeriodsDialog by remember { mutableStateOf(false) }

    // Silent Mode Settings
    val sharedPreferences = remember { context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE) }
    var autoMuteEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_mute_enabled", false)) }
    var silentModeType by remember { mutableStateOf(sharedPreferences.getInt("auto_mute_type", 0)) } // 0 = DND, 1 = Vibrate, 2 = Silent
    var preClassReminderOffset by remember { mutableStateOf(sharedPreferences.getInt("pre_class_reminder_offset", 10)) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

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
                Toast.makeText(context, "数据恢复成功！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // JSON export file saver activity launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = onExportJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Toast.makeText(context, "备份导出成功！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
            text = "系统设置与配置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 0. Course Table Import Section (Portal & Excel)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("一键导入课表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("支持通过教务系统（WebView 智能抓取）或导入 Excel/CSV 文件模板批量添加课程。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("前往导入课表页面", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Semester Section
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("学期管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                            Text("开学第一天: $formattedDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = "当前选择", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Button(
                    onClick = { showAddSemesterDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("新增学期")
                }
            }
        }

        // 2. Class Timings Table
        Card(
            shape = RoundedCornerShape(24.dp),
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
                    Text("作息时间表 (12节)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showEditPeriodsDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑时间")
                    }
                }

                periods.take(5).forEach { period ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("第 ${period.periodNumber} 节", style = MaterialTheme.typography.bodyMedium)
                        Text("${period.startTime} - ${period.endTime}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (periods.size > 5) {
                    Text("...... (其余课节时间点击上方编辑查看)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 3. Silent Mode Settings
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("上课静音自动化与课前提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用自动静音")
                    Switch(
                        checked = autoMuteEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                                    Toast.makeText(context, "请在跳转页面中授予“勿扰”访问权限！", Toast.LENGTH_LONG).show()
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
                    Text("静音模式选择", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("勿扰 (DND)", "仅振动", "完全静音").forEachIndexed { index, mode ->
                            val isSelected = silentModeType == index
                            OutlinedButton(
                                onClick = {
                                    silentModeType = index
                                    sharedPreferences.edit().putInt("auto_mute_type", index).apply()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("上课前提醒设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提醒时间", style = MaterialTheme.typography.bodyMedium)
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (preClassReminderOffset == 0) "准时提醒" else "提前 $preClassReminderOffset 分钟")
                        }
                        
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(0, 5, 10, 15, 20, 30).forEach { offset ->
                                DropdownMenuItem(
                                    text = { Text(if (offset == 0) "准时提醒" else "提前 $offset 分钟") },
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
                                                com.example.schday.data.AppDatabase.getDatabase(context)
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("备份与恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("schday_backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("导出备份")
                    }

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("恢复备份")
                    }
                }
            }
        }

        // 5. Data Reset & Debug Section
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("数据清理与演示", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("如果在首次进入时清空了演示数据，或希望彻底重置全部课表学期并重新载入示例数据，可使用下方功能：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onClearData()
                            Toast.makeText(context, "所有数据已成功清空！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("清空所有数据")
                    }

                    Button(
                        onClick = {
                            onLoadDemoData()
                            Toast.makeText(context, "示例演示数据已重新载入！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("加载示例数据")
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
            title = { Text("新增学期", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSemesterName,
                        onValueChange = { newSemesterName = it },
                        label = { Text("学期名称") },
                        placeholder = { Text("例如：2026 春季学期") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(newSemesterStartMillis))
                        Text("第一周周一: $formattedDate")
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
                    Text("添加", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // New Semester Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = newSemesterStartMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { newSemesterStartMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("选择")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Edit Period Times Dialog
    if (showEditPeriodsDialog) {
        var tempPeriods by remember(periods) { mutableStateOf(periods.map { it.copy() }) }

        AlertDialog(
            onDismissRequest = { showEditPeriodsDialog = false },
            title = { Text("修改作息时间表", fontWeight = FontWeight.Bold) },
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
                                Text("第 ${period.periodNumber} 节", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = period.startTime,
                                    onValueChange = { text ->
                                        tempPeriods = tempPeriods.mapIndexed { i, p ->
                                            if (i == idx) p.copy(startTime = text) else p
                                        }
                                    },
                                    placeholder = { Text("08:00") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                Text("-")
                                OutlinedTextField(
                                    value = period.endTime,
                                    onValueChange = { text ->
                                        tempPeriods = tempPeriods.mapIndexed { i, p ->
                                            if (i == idx) p.copy(endTime = text) else p
                                        }
                                    },
                                    placeholder = { Text("08:45") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
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
                    Text("保存作息", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPeriodsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("需要精确闹钟权限", fontWeight = FontWeight.Bold) },
            text = { Text("为了让自动静音模式和课前提醒准时工作，本应用需要使用系统“精确闹钟”权限。请在随后的设置中开启该权限。") },
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
                                Toast.makeText(context, "无法打开设置页面，请手动在系统设置中搜索授权。", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("去设置", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

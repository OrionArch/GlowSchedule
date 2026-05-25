package com.example.schday.ui.screens.import

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schday.data.DataRepository
import com.example.schday.data.entity.Course
import com.example.schday.data.entity.ScheduleSlot
import com.example.schday.parser.ExcelParser
import com.example.schday.parser.JSBridge
import com.example.schday.theme.MorandiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCoursesScreen(
    repository: DataRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentSemester by repository.getCurrentSemester().collectAsStateWithLifecycle(initialValue = null)

    var selectedTab by remember { mutableStateOf(0) } // 0 = CSV Import, 1 = Educational Web, 2 = JSON Text Paste

    // Excel/CSV import contract launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentSemester != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val parsed = ExcelParser.parseCsv(inputStream, currentSemester!!.id)
                        parsed.forEach { (course, slots) ->
                            repository.saveCourseWithSlots(course, slots)
                        }
                        Toast.makeText(context, "成功导入 ${parsed.size} 门课程！", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // WebView state variables
    var showWebView by remember { mutableStateOf(false) }
    var webUrl by remember { mutableStateOf("https://") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Fallback JSON Paste state
    var jsonText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课程表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showWebView) {
                            showWebView = false
                        } else {
                            onBack()
                        }
                    }) {
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
            if (!showWebView) {
                // Tab Selection Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CSV表格", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("教务网", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("JSON粘贴", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("AI截图", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        CsvImportPanel(
                            hasSemester = currentSemester != null,
                            onPickFile = { filePickerLauncher.launch("text/comma-separated-values") }
                        )
                    }
                    1 -> {
                        PortalImportPanel(
                            webUrl = webUrl,
                            onUrlChange = { webUrl = it },
                            onEnterPortal = { showWebView = true }
                        )
                    }
                    2 -> {
                        TextPasteImportPanel(
                            jsonText = jsonText,
                            onJsonChange = { jsonText = it },
                            onImport = {
                                if (currentSemester == null) return@TextPasteImportPanel
                                coroutineScope.launch {
                                    try {
                                        val array = org.json.JSONArray(jsonText)
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
                                                activeWeeks = obj.getString("weeks")
                                            )
                                            repository.saveCourseWithSlots(course, listOf(slot))
                                        }
                                        Toast.makeText(context, "成功解析并导入 ${array.length()} 门课程！", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "解析失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    3 -> {
                        AiScreenshotImportPanel(
                            hasSemester = currentSemester != null,
                            jsonText = jsonText,
                            onJsonChange = { jsonText = it },
                            onImport = {
                                if (currentSemester == null) return@AiScreenshotImportPanel
                                coroutineScope.launch {
                                    try {
                                        var sanitizedJson = jsonText.trim()
                                        if (sanitizedJson.startsWith("```")) {
                                            sanitizedJson = sanitizedJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                                        }
                                        val array = org.json.JSONArray(sanitizedJson)
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
                                            repository.saveCourseWithSlots(course, listOf(slot))
                                        }
                                        Toast.makeText(context, "AI 导入：成功同步 ${array.length()} 门课程！", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "解析 AI 数据失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // WebView UI showing school portal
                Box(modifier = Modifier.fillMaxSize()) {
                    PortalWebView(
                        url = webUrl,
                        repository = repository,
                        semesterId = currentSemester?.id ?: 0,
                        onCreated = { webViewInstance = it },
                        onSuccess = { count ->
                            Toast.makeText(context, "自动同步成功！成功同步 $count 门课程！", Toast.LENGTH_LONG).show()
                            showWebView = false
                            onBack()
                        }
                    )

                    // Overlay Floating Action Button to trigger injection scraper
                    ExtendedFloatingActionButton(
                        onClick = {
                            val script = getDOMScraperScript()
                            webViewInstance?.evaluateJavascript("javascript:$script", null)
                        },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text("开始抓取课表", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CsvImportPanel(hasSemester: Boolean, onPickFile: () -> Unit) {
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
            shape = RoundedCornerShape(12.dp),
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
fun PortalImportPanel(
    webUrl: String,
    onUrlChange: (String) -> Unit,
    onEnterPortal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌐 教务网智能同步", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "在下方输入您学校的教务系统登录网址（如正方教务系统），进入页面并登录您的学生账户。进入课表页后，点击底部的“开始抓取”按钮即可自动拉取课程表信息。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = webUrl,
            onValueChange = onUrlChange,
            label = { Text("教务网登录网址") },
            placeholder = { Text("https://jwxt.xxxx.edu.cn") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onEnterPortal,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("进入教务网")
        }
    }
}

@Composable
fun TextPasteImportPanel(
    jsonText: String,
    onJsonChange: (String) -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📝 黏贴 JSON 数据导入 (兜底方案)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "如果教务系统格式极度特殊，抓取不到，您可以通过在电脑端提取生成以下 JSON 格式复制过来黏贴导入：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = jsonText,
            onValueChange = onJsonChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("JSON 课表数组") },
            placeholder = { Text("[\n  {\n    \"name\": \"高等数学\",\n    \"teacher\": \"张教授\",\n    \"classroom\": \"教三302\",\n    \"day\": 1,\n    \"start\": 1,\n    \"end\": 2,\n    \"weeks\": \"1,2,3,4,5,6,7,8\"\n  }\n]") },
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = onImport,
            enabled = jsonText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("解析并同步")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PortalWebView(
    url: String,
    repository: DataRepository,
    semesterId: Int,
    onCreated: (WebView) -> Unit,
    onSuccess: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                // Add JS Bridge
                addJavascriptInterface(
                    JSBridge { parsedList ->
                        coroutineScope.launch {
                            var colorIdx = 0
                            parsedList.forEach { p ->
                                val course = Course(
                                    semesterId = semesterId,
                                    name = p.name,
                                    teacher = p.teacher,
                                    colorHex = MorandiColors[colorIdx % MorandiColors.size]
                                )
                                colorIdx++
                                val slot = ScheduleSlot(
                                    courseId = 0,
                                    dayOfWeek = p.day,
                                    startPeriod = p.start,
                                    endPeriod = p.end,
                                    classroom = p.classroom,
                                    activeWeeks = p.weeks
                                )
                                repository.saveCourseWithSlots(course, listOf(slot))
                            }
                            onSuccess(parsedList.size)
                        }
                    },
                    "AndroidBridge"
                )
                
                onCreated(this)
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Returns the JavaScript DOM scraper.
 * This scans common table cell formats or loops through grid elements to extract 
 * name, classroom, teacher, day, periods, and active weeks, passing them back to AndroidBridge.
 */
private fun getDOMScraperScript(): String {
    return """
    (function() {
        var results = [];
        // Loop through all table cells (td)
        var tds = document.getElementsByTagName('td');
        for (var i = 0; i < tds.length; i++) {
            var cell = tds[i];
            var html = cell.innerHTML;
            var text = cell.innerText.trim();
            
            // Check if cell seems to contain course data (name + classroom patterns)
            if (text.length > 5 && (text.includes('教室') || text.includes('第') || text.includes('周') || text.includes('\n'))) {
                // Find column/day of week index from its horizontal position or parent tr cell index
                var dayIndex = cell.cellIndex;
                if (dayIndex === undefined) {
                    dayIndex = 1;
                }
                
                // Extract lines
                var lines = text.split('\n').map(function(l) { return l.trim(); }).filter(function(l) { return l.length > 0; });
                if (lines.length >= 2) {
                    var courseName = lines[0];
                    var classroom = "";
                    var teacher = "";
                    var weeks = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16";
                    
                    // Simple regex mapping classroom and week patterns
                    for (var j = 1; j < lines.length; j++) {
                        var line = lines[j];
                        if (line.includes('周')) {
                            // Guess weeks e.g. "1-16周"
                            var matches = line.match(/(\d+)-(\d+)/);
                            if (matches && matches.length >= 3) {
                                var start = parseInt(matches[1]);
                                var end = parseInt(matches[2]);
                                var list = [];
                                for (var w = start; w <= end; w++) {
                                    list.push(w);
                                }
                                weeks = list.join(',');
                            }
                        } else if (line.includes('室') || line.includes('楼') || /^[A-Z]\d+/.test(line)) {
                            classroom = line;
                        } else {
                            teacher = line;
                        }
                    }
                    
                    // Try to guess start/end periods from cell index, row positions or defaults
                    var startPeriod = 1;
                    var endPeriod = 2;
                    var parentRow = cell.parentNode;
                    if (parentRow && parentRow.rowIndex !== undefined) {
                        startPeriod = parentRow.rowIndex;
                        endPeriod = startPeriod + (cell.rowSpan ? cell.rowSpan - 1 : 1);
                    }
                    
                    results.push({
                        name: courseName,
                        teacher: teacher,
                        classroom: classroom,
                        day: dayIndex > 0 ? dayIndex : 1,
                        start: startPeriod,
                        end: endPeriod,
                        weeks: weeks
                    });
                }
            }
        }
        
        // Return JSON string array to AndroidBridge
        AndroidBridge.sendCourseData(JSON.stringify(results));
    })();
    """.trimIndent()
}

@Composable
fun AiScreenshotImportPanel(
    hasSemester: Boolean,
    jsonText: String,
    onJsonChange: (String) -> Unit,
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
                text = "您可以对任何其他软件或网页上的课表进行截图。复制下方精心定制的 AI 提示词，并连同截图一起发送给多模态 AI（如 ChatGPT、Claude、Gemini），AI 识别后会将课表转换为 JSON 格式，您将其粘贴在下方即可快速导入！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                        shape = RoundedCornerShape(8.dp),
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
                shape = RoundedCornerShape(12.dp)
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

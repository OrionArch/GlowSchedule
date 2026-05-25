<!-- Parent: ../AGENTS.md -->

# ui/screens/import/

Multi-method course import screen.

## Key Files

| File | Purpose |
|------|---------|
| `ImportCoursesScreen.kt` | Tabbed import screen with four methods plus an embedded WebView. |

## Import Methods (Tabs)

| Tab | Composable | Description |
|-----|------------|-------------|
| CSV | `CsvImportPanel` | File picker for CSV files. Parsed by `ExcelParser.parseCsv()`. |
| Portal | `PortalImportPanel` + `PortalWebView` | Opens a WebView to the school's educational portal. A floating "Start Scraping" button injects a DOM scraper JS script that extracts course data from HTML tables and sends it back via `JSBridge`. |
| JSON Paste | `TextPasteImportPanel` | Text field for pasting a JSON array of course objects. Directly parses and saves. |
| AI Screenshot | `AiScreenshotImportPanel` | Provides a copyable multimodal AI prompt. User pastes AI-generated JSON output. Handles markdown code fences (strips ```json``` wrappers). |

## WebView Scraping

- `getDOMScraperScript()` returns inline JavaScript that scans `<td>` elements for course-like content, extracts name/classroom/teacher/weeks, and calls `AndroidBridge.sendCourseData()`.
- `JSBridge` receives the JSON and saves courses via repository on a coroutine.

## Dependencies

- `ExcelParser` for CSV parsing
- `JSBridge` for WebView communication
- `MorandiColors` for auto-assigned course colors
- `DataRepository` for saving parsed courses

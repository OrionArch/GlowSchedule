<!-- Parent: ../AGENTS.md -->

# parser/

Import, export, and sharing utilities for course data.

## Key Files

| File | Purpose |
|------|---------|
| `ExcelParser.kt` | Parses CSV input streams into `Course` + `ScheduleSlot` pairs. Expects header row; columns: name, teacher, classroom, dayOfWeek, startPeriod, endPeriod, week numbers. Assigns Morandi colors round-robin. |
| `BackupRestore.kt` | Full JSON backup/export and restore/import. `exportToJson()` serializes semesters, courses (with slots and homework), and period times to a versioned JSON structure. `importFromJson()` re-maps old semester IDs to new ones during import. |
| `JSBridge.kt` | `@JavascriptInterface` class for WebView-to-Android communication. Receives parsed course JSON arrays from injected DOM scraper scripts. Defines `ParsedCourse` data class. |
| `GlowCodeManager.kt` | Encodes/decodes shareable "Glow Codes" (`glow://` + Base64 JSON). Used for peer-to-peer timetable sharing via clipboard. Defines `SharedCourse`, `SharedSlot`, and `GlowCodeData` data classes. |

## Data Flow

1. **CSV Import**: `ExcelParser.parseCsv()` -> list of `(Course, List<ScheduleSlot>)` -> `repository.saveCourseWithSlots()`
2. **WebView Import**: `JSBridge.sendCourseData()` callback <- JS DOM scraper -> `ParsedCourse` list -> repository save
3. **Glow Code**: Clipboard `glow://` string -> `GlowCodeManager.decode()` -> `GlowCodeData` -> `MainScreenViewModel.importGlowCode()`
4. **Backup**: `BackupRestore.exportToJson()` -> JSON file via SAF; `BackupRestore.importFromJson()` <- JSON file via SAF

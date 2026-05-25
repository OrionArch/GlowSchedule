<!-- Parent: ../AGENTS.md -->

# ui/screens/settings/

App settings, semester management, class timing configuration, and data backup/restore.

## Key Files

| File | Purpose |
|------|---------|
| `SettingsTab.kt` | Scrollable settings screen organized into card sections. Uses `ActivityResultContracts` for file import/export via SAF (Storage Access Framework). |

## Settings Sections

| Section | Description |
|---------|-------------|
| Import Courses | Navigation button to `ImportCoursesScreen`. |
| Semester Management | Lists all semesters with current-selection indicator. "Add Semester" dialog with name and date picker (auto-adjusts to Monday). |
| Class Timings Table | Displays period times (first 5 shown, expandable). Editable via a dialog with inline text fields for each period's start/end time. |
| Auto-Mute & Reminders | Toggle for auto-mute with DND permission check. Mode selector: DND / Vibrate / Silent. Pre-class reminder offset dropdown (0/5/10/15/20/30 min). Exact alarm permission dialog for API 31+. |
| Backup & Restore | Export to JSON file (`CreateDocument` contract), import from JSON file (`GetContent` contract). Uses `BackupRestore` from parser package. |
| Data Reset & Demo | "Clear All Data" deletes all semesters (cascading). "Load Demo Data" re-inserts the built-in demo dataset. |

## Permissions Handled

- `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` for DND on API 23+
- `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` for exact alarms on API 31+

## SharedPreferences

Reads/writes `schday_settings` for auto-mute toggle, mute type, and reminder offset values.

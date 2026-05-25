# com.example.schday

<!-- Parent: ../../../../../../AGENTS.md -->

Main source package for GlowSchedule. Contains the application entry point, navigation graph, and all feature packages.

## Architecture

MVVM with Repository pattern. Room provides local persistence. Jetpack Compose drives the UI layer. ViewModels expose state to composables via `StateFlow` / `collectAsState()`.

## Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Application entry point; sets up splash screen and Compose content |
| `Navigation.kt` | Navigation graph defining all routes and screen transitions |
| `NavigationKeys.kt` | Route constants and navigation parameter keys |

## Subdirectories

| Directory | Description |
|-----------|-------------|
| `data/` | Room database, DAOs, entities, and `DataRepository` |
| `parser/` | Excel import parsing, backup/restore, JS bridge, GlowCode management |
| `scheduler/` | Class alarm scheduling (`ClassAlarmReceiver`) |
| `theme/` | Compose theme definitions: colors, typography, overall theme |
| `ui/` | Compose screens and ViewModels organized by feature |
| `utils/` | Date/time utility functions |
| `widget/` | Glance-based home screen widget (`ScheduleWidget`) |

## Package Detail

### `data/`

| File | Purpose |
|------|---------|
| `AppDatabase.kt` | Room database definition |
| `DataRepository.kt` | Single repository exposing data operations to ViewModels |
| `dao/AppDaos.kt` | Room DAO interfaces for database queries |
| `entity/Entities.kt` | Room entity / data class definitions |

### `parser/`

| File | Purpose |
|------|---------|
| `ExcelParser.kt` | Parses class schedule data from Excel files |
| `BackupRestore.kt` | Handles database backup and restore |
| `GlowCodeManager.kt` | Manages GlowCode (schedule sharing code) generation and parsing |
| `JSBridge.kt` | JavaScript bridge for WebView-based schedule import |

### `scheduler/`

| File | Purpose |
|------|---------|
| `ClassAlarmReceiver.kt` | BroadcastReceiver for scheduling class reminders |

### `theme/`

| File | Purpose |
|------|---------|
| `Color.kt` | Color palette definitions |
| `Theme.kt` | Material 3 theme composable (light/dark) |
| `Type.kt` | Typography scale definitions |

### `ui/`

| File | Purpose |
|------|---------|
| `main/MainScreen.kt` | Root screen with bottom navigation |
| `main/MainScreenViewModel.kt` | ViewModel for main screen state |
| `screens/home/HomeTab.kt` | Home / dashboard tab |
| `screens/timetable/TimetableTab.kt` | Weekly timetable view |
| `screens/todo/TodoTab.kt` | Task / to-do list tab |
| `screens/edit/AddEditCourseScreen.kt` | Add or edit a course |
| `screens/import/ImportCoursesScreen.kt` | Import courses from file |
| `screens/settings/SettingsTab.kt` | App settings |

### `utils/`

| File | Purpose |
|------|---------|
| `DateUtils.kt` | Date and time helper functions |

### `widget/`

| File | Purpose |
|------|---------|
| `ScheduleWidget.kt` | Glance AppWidget showing today's schedule on the home screen |

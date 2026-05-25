<!-- Parent: ../AGENTS.md -->

# ui/main/

Main screen shell: bottom navigation, tab content routing, and central ViewModel.

## Key Files

| File | Purpose |
|------|---------|
| `MainScreen.kt` | Root composable. Renders a custom bottom navigation bar (4 tabs: Home, Timetable, Todo, Settings) and swaps tab content. Handles Glow Code clipboard detection via `DisposableEffect` on `ON_RESUME`. Shows a confirmation dialog when a `glow://` code is found. Reschedules alarms reactively when courses or periods change via `LaunchedEffect`. |
| `MainScreenViewModel.kt` | Central `ViewModel` exposing `StateFlow` properties for `currentTab`, `selectedWeek`, `semesters`, `currentSemester`, `periodTimes`, `courses`, and `allHomework`. Initializes default period times (12 periods, 08:00-20:05) and demo data on first launch. Provides mutation methods for homework, courses, semesters, and Glow Code import. |

## Tab Index Mapping

| Index | Tab | Composable |
|-------|-----|------------|
| 0 | Home | `HomeTab` |
| 1 | Timetable | `TimetableTab` |
| 2 | Todo | `TodoTab` |
| 3 | Settings | `SettingsTab` |

## State Flow

All data flows from `DataRepository` through `MainScreenViewModel` (via `stateIn`) to composables via `collectAsStateWithLifecycle()`. Child tabs receive data as parameters and report user actions via callbacks.

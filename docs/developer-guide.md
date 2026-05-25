# Developer Guide

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17 | Eclipse Temurin recommended |
| Android SDK | API 36 | Install via Android Studio SDK Manager |
| Android Studio | Latest stable | Meerkat or newer for AGP 9.0 |
| Git | 2.x | |

## Setup

```bash
git clone https://github.com/OrionArch/GlowSchedule.git
cd GlowSchedule
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
GlowSchedule/
  app/                         # Single application module
    src/main/java/com/example/schday/
      MainActivity.kt          # Entry point
      Navigation.kt            # Navigation 3 graph
      NavigationKeys.kt        # NavKey definitions
      data/                    # Data layer (Room + Repository)
      parser/                  # Import/export utilities
      scheduler/               # Alarm scheduling
      theme/                   # Material 3 theme
      ui/                      # Compose UI screens
      utils/                   # Shared utilities
      widget/                  # Glance widget
    src/test/                  # Unit tests
    src/androidTest/           # Instrumented tests
  gradle/
    libs.versions.toml         # Version catalog
  mockups/                     # Design assets
```

## Build Variants

| Variant | Command | Notes |
|---------|---------|-------|
| Debug | `./gradlew assembleDebug` | Debuggable, no minification |
| Release | `./gradlew assembleRelease` | R8/ProGuard minification enabled |

## Testing

```bash
./gradlew test                    # Unit tests (JVM)
./gradlew connectedAndroidTest    # Instrumented tests (device/emulator required)
```

- Unit tests: `app/src/test/java/com/example/schday/`
- Instrumented tests: `app/src/androidTest/java/com/example/schday/`

## Key Conventions

- **UI:** All screens are Jetpack Compose composables. No XML layouts.
- **Reactive data:** Kotlin `Flow` throughout the data layer. ViewModels expose `StateFlow` collected with `collectAsStateWithLifecycle()`.
- **Dependency injection:** Manual. `AppDatabase` is created in `MainActivity` and passed as `DataRepository` to composables via parameters.
- **ViewModels:** One per screen. Created with `viewModel { }` blocks, receive `DataRepository` as a constructor parameter.
- **Navigation:** Navigation 3 with `NavKey` data objects. Back stack managed via `rememberNavBackStack(Main)`.
- **Database:** Room with `fallbackToDestructiveMigration()`. Schema version 1, export disabled.

## Common Tasks

### Add a New Tab Screen

1. Create a new composable in `ui/screens/<name>/` (e.g., `ui/screens/notes/NotesTab.kt`).
2. Add a tab entry in `MainScreen.kt` with an icon and label.
3. The composable receives `DataRepository` as a parameter -- use it to create or access a ViewModel.
4. No new `NavKey` is needed unless the screen requires its own back-stack entry.

### Add a New Database Entity

1. Define the entity data class in `data/entity/Entities.kt` with `@Entity`, `@PrimaryKey`, and any `@ForeignKey` annotations.
2. Add the entity class to the `entities` array in `AppDatabase.kt`.
3. Create a DAO interface in `data/dao/AppDaos.kt` (or add methods to an existing DAO).
4. Add the DAO abstract method to `AppDatabase`.
5. Add corresponding methods to the `DataRepository` interface and `DefaultDataRepository` implementation in `DataRepository.kt`.
6. **Important:** Bump `version` in `@Database` and either add a migration or keep `fallbackToDestructiveMigration()` (which will wipe existing data).

### Modify the Glance Widget

Edit `widget/ScheduleWidget.kt`. The widget:
- Creates its own `DefaultDataRepository` instance (does not share with the main app process).
- Runs in `provideGlance()` which is a coroutine context.
- Must use Glance-specific composables (`GlanceModifier`, `Text`, `Column`, `Row`), not standard Compose.

After changes, the widget updates on the next system trigger. You can force an update by calling `ScheduleWidget().updateAll(context)`.

### Add a New Alarm Action

1. Define a new action string in `ClassAlarmReceiver.companion` (e.g., `const val ACTION_MY_ACTION = "com.example.schday.ACTION_MY_ACTION"`).
2. Add a `when` branch in `ClassAlarmReceiver.onReceive()` to handle the action.
3. Schedule the alarm in `AlarmScheduler` using `scheduleAlarm(alarmManager, triggerTime, pendingIntent)`.
4. Register the action string in `AndroidManifest.xml` if it uses a new `<intent-filter>`.

### Add a New Import Source

1. Create a parser in `parser/` (e.g., `parser/IcsParser.kt`).
2. The parser should output domain objects (`Course`, `ScheduleSlot`) that can be saved via `DataRepository.saveCourseWithSlots()`.
3. Wire the parser into `ImportCoursesScreen.kt` or `SettingsTab.kt` with a file picker or text input.

## Known Limitations

- **Hardcoded Chinese strings:** Most UI text is inlined in composables rather than in `strings.xml`. Only `app_name` is properly externalized. Internationalization requires extracting all strings to resource files.
- **Package name:** `com.example.schday` is a placeholder package name, not a proper reverse-domain name. Changing it requires a full refactor and will break existing installations.
- **Room schema version 1 with destructive migration:** Any entity change will wipe all user data. A production release should implement proper Room migrations and enable schema export.
- **No offline sync:** The app is fully local -- no cloud sync, no account system. Backup/restore is manual JSON export/import.
- **Widget process isolation:** The Glance widget creates its own `AppDatabase` instance and does not share state with the main app process beyond the shared SQLite file.

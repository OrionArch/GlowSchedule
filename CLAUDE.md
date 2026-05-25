# GlowSchedule - Project Guide

Android class schedule app built with Kotlin, Jetpack Compose, and Room.

## Build & Run

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK
./gradlew lintDebug          # Lint checks
```

## Test

```bash
./gradlew test                      # Unit tests
./gradlew connectedAndroidTest      # Instrumented tests (requires device/emulator)
```

## Architecture

MVVM + Repository pattern, single `:app` module.

```
app/src/main/java/.../
├── data/
│   ├── entity/          # Room entities
│   ├── dao/             # Room DAOs
│   ├── AppDatabase.kt   # Room database singleton
│   └── DataRepository.kt# Single repository
├── ui/
│   ├── screens/         # Compose screens (one file per tab)
│   ├── main/
│   │   └── MainScreen.kt  # NavHost (Navigation 3)
│   └── theme/           # Material 3 (Color.kt, Theme.kt, Type.kt)
└── widget/
    └── ScheduleWidget.kt  # Glance widget
```

## Code Conventions

- Kotlin official code style
- Material 3 components throughout
- Compose state hoisting pattern
- Kotlin `Flow` for reactive data streams
- KAPT for Room annotation processing

## SDK & Toolchain

- minSdk 24 / targetSdk 36 / compileSdk 36
- JDK 17 (Temurin)

## Git Branching Strategy

Trunk-Based Development with short-lived release branches (no permanent `develop` branch).

**Long-lived branches**: `main` only — always buildable and testable.

| Type | Naming | Lifetime | Example |
|---|---|---|---|
| Feature | `feature/<name>` | 1-3 days, merge & delete | `feature/week-view` |
| Bugfix | `fix/<name>` | Hours, merge & delete | `fix/date-picker-crash` |
| Chore | `chore/<name>` | Short, merge & delete | `chore/upgrade-compose` |
| Release | `release/v<semver>` | During stabilization, delete after deploy | `release/v1.1.0` |
| Hotfix | `hotfix/v<semver>` | Emergency, delete after deploy | `hotfix/v1.0.1` |

**Rules**:
- All work on `main` goes through PR (squash merge)
- Feature branches live max 2-3 days; split longer work into smaller pieces
- Release branches are cut from `main`, tagged, and deleted after production deploy
- Hotfixes: fix on `main` first, cherry-pick to release branch if one exists
- Version tags: `v<MAJOR>.<MINOR>.<PATCH>` (Semantic Versioning)
- Commit messages: Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`)

## Constraints

- Do NOT add new Gradle modules
- Do NOT switch Room compiler from KAPT to KSP
- Do NOT modify ProGuard rules without testing a release build

## Common Tasks

### Add a new tab screen
1. Create composable function in `ui/screens/<name>/<Name>Tab.kt`
2. Add NavKey data class in `NavigationKeys.kt`
3. Register route in `Navigation.kt` entryProvider
4. Add tab to MainScreen.kt bottom navigation

### Add a new database entity
1. Define @Entity data class in `data/entity/Entities.kt`
2. Add @Dao interface methods in `data/dao/AppDaos.kt`
3. Add entity to AppDatabase.kt entities array (increment version + add migration)
4. Add repository methods in `DataRepository.kt`

### Modify the Glance widget
1. Edit `widget/ScheduleWidget.kt` for UI changes
2. Update Glance state provider if needed
3. Widget metadata in `res/xml/schedule_widget_info.xml`

### Add a new alarm action
1. Define action string constant in `scheduler/ClassAlarmReceiver.kt`
2. Add handling case in onReceive()
3. Schedule via AlarmScheduler methods

## Known Gotchas

- UI strings are hardcoded in Chinese — not in `strings.xml` (only `app_name` exists there)
- Package name is `com.example.schday` — not a proper reverse-domain name
- Room schema version is 1 — entity changes require a Migration class
- Room uses KAPT (not KSP) — do not switch (project constraint)
- ProGuard minification is enabled for release builds — test release builds before shipping

## Key Patterns

- **Navigation 3**: NavKey sealed class + `rememberNavBackStack` + `NavDisplay` with `entryProvider` mapping
- **Glance Widget**: `GlanceAppWidget` subclass + `GlanceAppWidgetReceiver` + state management via `updateAppWidgetState`
- **AlarmManager**: Exact alarms via `AlarmManager.setAlarmClock()` with `PendingIntent` to `BroadcastReceiver` + `BootReceiver` for reschedule on reboot
- **Reactive Data**: Kotlin `Flow` from Room DAO → DataRepository interface → ViewModel `StateFlow` → Compose collection

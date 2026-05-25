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

## Constraints

- Do NOT add new Gradle modules
- Do NOT switch Room compiler from KAPT to KSP
- Do NOT modify ProGuard rules without testing a release build

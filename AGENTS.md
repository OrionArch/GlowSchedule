# GlowSchedule

Android class schedule app built with Kotlin, Jetpack Compose, Material 3, Room, Navigation 3, and Jetpack Glance.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose (BOM 2026.03.01), Material 3 |
| Navigation | Navigation 3 (1.0.1) |
| Persistence | Room 2.8.4 |
| Widgets | Jetpack Glance 1.1.0 |
| Architecture | MVVM with Repository pattern |
| Build | AGP 9.0.1, Gradle configuration cache enabled |

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Root build script; declares plugins (Compose compiler, Kotlin serialization, KAPT) |
| `settings.gradle.kts` | Project name `GlowSchedule`, includes `:app` module |
| `gradle.properties` | JVM args, AndroidX, non-transitive R classes, configuration cache |
| `gradle/libs.versions.toml` | Version catalog for all dependencies and plugins |

## Subdirectories

| Directory | Description |
|-----------|-------------|
| `app/` | Main application module (see [app/AGENTS.md](app/AGENTS.md)) |
| `gradle/` | Gradle wrapper and version catalog |
| `mockups/` | Design mockups and icon generation utilities (see [mockups/AGENTS.md](mockups/AGENTS.md)) |

## Build

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (minified with ProGuard)
```

## Test

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

# app

<!-- Parent: ../AGENTS.md -->

Main application module for GlowSchedule.

## Build Config

| Property | Value |
|----------|-------|
| Namespace | `com.example.schday` |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 36 |
| Java compatibility | 17 |
| Compose | Enabled |
| Release minification | Enabled (ProGuard) |

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Module build script with all dependency declarations |
| `proguard-rules.pro` | ProGuard rules for release builds |

## Subdirectories

| Directory | Description |
|-----------|-------------|
| `src/main/java/com/example/schday/` | Kotlin source code (see [schday/AGENTS.md](src/main/java/com/example/schday/AGENTS.md)) |
| `src/main/res/` | Android resources (layouts, strings, drawables, themes) |
| `src/test/` | Unit tests |
| `src/androidTest/` | Instrumented tests |

## Notable Dependencies

- **Compose BOM 2026.03.01** — manages Compose library versions
- **Navigation 3** — `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`
- **Room** — `room-runtime`, `room-ktx`, `room-compiler` (via KAPT)
- **Glance** — `glance`, `glance-appwidget` for home screen widgets
- **Splash Screen** — `androidx.core:core-splashscreen:1.0.1`
- **Material Icons** — core and extended icon sets

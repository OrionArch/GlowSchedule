# Changelog

All notable changes to GlowSchedule will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.0] - 2026-05-26

### Added

- Multi-theme selector with multiple color schemes
- Custom date picker components with improved interaction
- Now & Next Glance widget showing current/upcoming class
- Course clash detection dialog when adding overlapping courses

### Changed

- Compact 2×1 schedule widget with progress indicator
- Widget auto-refreshes on data changes (courses, semesters, homework)

### Removed

- Physics sandbox widget from HomeTab

## [1.0.1] - 2026-05-25

### Added

- Release signing configuration
- DateUtils unit tests
- Git branching strategy documentation

### Changed

- Refactored ImportCoursesScreen with cleaner parsing logic
- Enhanced DateUtils with improved date handling

## [1.0.0] - 2025-05-25

### Added

- Class schedule management (add, edit, delete courses)
- Home screen overview with today's schedule
- Weekly timetable grid view
- Todo/task management for courses
- Import courses from Excel files
- Import courses from educational portals via WebView
- GlowCode management for course code parsing
- Class reminder alarms via BroadcastReceiver
- Home screen widget via Jetpack Glance
- Material 3 dynamic theming with dark mode support
- Data backup and restore functionality
- Settings screen with app configuration
- Room database for local data persistence
- Navigation 3 based navigation
- ProGuard minification for release builds

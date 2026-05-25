<!-- Parent: ../AGENTS.md -->

# ui/screens/

Feature screen composables. Each subdirectory is a self-contained screen module.

## Subdirectories

| Directory | Screen | Key File | Purpose |
|-----------|--------|----------|---------|
| `edit/` | Add/Edit Course | `AddEditCourseScreen.kt` | Form for creating or editing a course with time slots, color, and week selection |
| `home/` | Home | `HomeTab.kt` | Today's schedule overview with countdown, timeline, and smart widgets |
| `import/` | Import Courses | `ImportCoursesScreen.kt` | Multi-tab import: CSV, WebView portal, JSON paste, AI screenshot |
| `settings/` | Settings | `SettingsTab.kt` | Semester management, period times editor, auto-mute config, backup/restore, data reset |
| `timetable/` | Timetable | `TimetableTab.kt` | Weekly grid view with HorizontalPager, conflict resolution, and course details bottom sheet |
| `todo/` | Todo | `TodoTab.kt` | Homework/task checklist with quick-add, countdown badges, and particle animations |

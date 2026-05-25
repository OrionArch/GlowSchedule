<!-- Parent: ../AGENTS.md -->

# widget/

Home screen widget displaying today's course schedule.

## Key Files

| File | Purpose |
|------|---------|
| `ScheduleWidget.kt` | Jetpack Glance-based home screen widget. Shows today's courses for the current semester (up to 3 items). |

## Classes

| Class | Purpose |
|-------|---------|
| `ScheduleWidget` | `GlanceAppWidget` implementation. Queries `DataRepository` in `provideGlance()` for current semester, courses, and period times. Renders "Today's Schedule (Week N)" header with up to 3 course entries (start time, name, classroom). Shows "No classes today" when empty or "No semester info" when no semester exists. |
| `ScheduleWidgetReceiver` | `GlanceAppWidgetReceiver` that provides the `ScheduleWidget` instance to the system. |

## Data Access

- Directly instantiates `AppDatabase` and `DefaultDataRepository` (not injected).
- Uses `DateUtils.getCurrentWeek()` and `DateUtils.getDayOfWeek()` to filter today's active slots.
- Sorted by `startPeriod` ascending.

## Limitations

- Shows a maximum of 3 course entries due to widget size constraints.
- No manual refresh trigger; relies on Glance's automatic update cycle.

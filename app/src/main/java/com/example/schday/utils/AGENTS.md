<!-- Parent: ../AGENTS.md -->

# utils/

Date and time utility functions used across the app.

## Key Files

| File | Purpose |
|------|---------|
| `DateUtils.kt` | Singleton object with helper methods for week calculations, day-of-week mapping, and active-week parsing. |

## Functions

| Function | Signature | Description |
|----------|-----------|-------------|
| `getCurrentWeek` | `(startDateMillis: Long, totalWeeks: Int) -> Int` | Computes current semester week (1-based) from semester start date. Clamped to `[1, totalWeeks]`. |
| `getDayOfWeek` | `() -> Int` | Returns current day of week as 1=Monday through 7=Sunday (converts from `Calendar.SUNDAY=1` convention). |
| `parseActiveWeeks` | `(activeWeeksStr: String) -> List<Int>` | Parses comma-separated week string (e.g. `"1,2,3,4,5"`) into integer list. |
| `isWeekActive` | `(activeWeeksStr: String, week: Int) -> Boolean` | Checks if a given week number is in the active weeks list. |
| `formatPeriodTime` | `(startTime: String, endTime: String) -> String` | Formats period time range as `"HH:mm-HH:mm"`. |
| `getDayName` | `(dayOfWeek: Int) -> String` | Returns Chinese day name string (e.g. `1 -> "周一"`, `7 -> "周日"`). |

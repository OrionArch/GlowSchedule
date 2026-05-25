<!-- Parent: ../AGENTS.md -->

# ui/screens/timetable/

Weekly timetable grid view with swipe paging and conflict resolution.

## Key Files

| File | Purpose |
|------|---------|
| `TimetableTab.kt` | Full timetable screen with `HorizontalPager` for week navigation, grid rendering, and a modal bottom sheet for course details. |

## Key Composables

| Composable | Purpose |
|------------|---------|
| `TimetableTab` | Top-level screen. Week selector chip with dropdown, pager for week swiping, toggle switches for "Show current week only" and "Hide weekends". |
| `TimetableGrid` | Absolute-positioned grid using `BoxWithConstraints`. Renders period rows (1-12), day columns (5 or 7), and course cards placed by their day/period coordinates. Detects overlapping courses and groups them. |
| `DaysHeaderRow` | Column headers (Mon-Sun or Mon-Fri) with today indicator dot. |
| `DetailItem` | Helper row for bottom sheet detail display (icon + label + value). |

## Conflict Resolution

- Overlapping slots on the same day/period are grouped and rendered with a 3D stacked-card offset effect.
- A `topCourseIds` state map tracks user-selected "top" courses per cell.
- Bottom sheet shows a `ScrollableTabRow` to switch between conflicting courses, with a "Pin to Top" button.

## Display Logic

- `DisplaySlot` data class wraps a course-slot pair with computed `isActive` and `hasPendingHomework` flags.
- Inactive courses (not in current week) are rendered at 45% opacity with a "Non-current week" badge.
- Courses with pending homework show a red dot indicator.

## Data

- `DisplaySlot` data class (defined in this file)
- `Modifier.scale()` extension for compact UI switches

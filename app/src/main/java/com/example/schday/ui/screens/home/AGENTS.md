<!-- Parent: ../AGENTS.md -->

# ui/screens/home/

Home screen with today's schedule overview, countdown timer, and contextual widgets.

## Key Files

| File | Purpose |
|------|---------|
| `HomeTab.kt` | Main home composable and several helper composables/widgets. Approximately 1000 lines. |

## Composables

| Composable | Purpose |
|------------|---------|
| `HomeTab` | Top-level screen. Shows welcome header with date, countdown card, widgets, and a vertical timeline of today's courses. Includes a FAB to add new courses. |
| `CountdownCard` | Frosted-glass card showing countdown to next class (or remaining time in current class). Animated pulse indicator: green=active, amber=upcoming, gray=done. |
| `CourseTimelineItem` | Single course row in the timeline. Left: start/end time. Center: timeline node (animated pulse if active, filled if past, outlined if future). Right: course card with Morandi background color. |
| `DndWidget` | Quick-toggle widget showing auto-mute status with a "Extend 1 hour" button. |
| `TransitAdvisorWidget` | Contextual card shown when consecutive classes are in different buildings. |
| `SentimentWallWidget` | Post-class mood emoji picker (appears within 30 minutes of class ending). Persists selection per course per day. |

## Helper Functions

- `getCurrentMinutes()`: Returns current time as minutes since midnight.
- `timeStringToMinutes(timeStr)`: Parses "HH:mm" string to minutes.
- `computeCountdown(...)`: Determines active/next course and computes countdown text.

## Data

- `CountdownStatus` data class holds the countdown card state.
- Updates every 15 seconds via `LaunchedEffect` loop.

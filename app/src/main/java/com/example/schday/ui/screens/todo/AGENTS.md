<!-- Parent: ../AGENTS.md -->

# ui/screens/todo/

Homework and task management screen.

## Key Files

| File | Purpose |
|------|---------|
| `TodoTab.kt` | Task checklist composable with quick-add form, countdown badges, and particle completion animations. |

## Key Composables

| Composable | Purpose |
|------------|---------|
| `TodoTab` | Main screen. Quick-add form with course selector dropdown and date picker. Split into "In Progress" and "Completed" sections. |
| `TodoItemRow` | Individual task card. Shows task title, course tag (colored badge), deadline countdown, and delete button. |
| `BouncyCheckbox` | Custom animated checkbox with spring-based scale animation on check/uncheck. |

## Features

- **Quick add**: Course dropdown selector + date picker + title field. Requires both course selection and non-blank title.
- **Countdown badges**: "Today", "Tomorrow", "N days left", "Overdue", or "Completed" with color-coded text (red=urgent, orange=tomorrow, primary=normal).
- **Completion animation**: Particle explosion effect (15 particles with gravity) on task completion. Haptic feedback via `Vibrator`.
- **Strikethrough styling**: Completed tasks show line-through text and reduced opacity.

## Data

- Receives `List<CourseWithSchedules>` and `List<Homework>` from parent.
- Homework CRUD operations communicated via `onAddHomework`, `onUpdateHomework`, `onDeleteHomework` callbacks.

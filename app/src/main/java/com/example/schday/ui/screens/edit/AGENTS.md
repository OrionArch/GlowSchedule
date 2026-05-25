<!-- Parent: ../AGENTS.md -->

# ui/screens/edit/

Add and edit course form screen.

## Key Files

| File | Purpose |
|------|---------|
| `AddEditCourseScreen.kt` | Full-featured course editor composable. Creates new courses or loads existing ones via `repository.getCourseById()`. Supports multiple time slots per course. |

## Features

- **Basic info**: Course name (required) and teacher name fields.
- **Color picker**: 8 Morandi color circles with selection indicator.
- **Time slot builder**: Day-of-week selector (7 buttons), start/end period dropdowns (1-12), classroom text field. Supports adding multiple slots and deleting individual ones.
- **Week selector dialog**: 4x5 grid of checkboxes for weeks 1-20. Quick presets: Select All, Odd Weeks, Even Weeks, Clear.
- **Save**: Validates non-blank name, calls `repository.saveCourseWithSlots()` in a coroutine.
- **Delete**: Shown only when editing an existing course. Calls `repository.deleteCourse()`.
- **State**: Uses `SlotInput` data class to hold mutable form state; loads from DB once via `isLoaded` flag.

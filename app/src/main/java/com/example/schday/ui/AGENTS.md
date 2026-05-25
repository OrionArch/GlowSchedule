<!-- Parent: ../AGENTS.md -->

# ui/

Compose UI layer. Contains the main screen shell and all feature screens.

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `main/` | `MainScreen` composable (bottom navigation, tab routing, Glow Code clipboard detection) and `MainScreenViewModel` (central state holder). |
| `screens/` | Feature screen composables organized by domain: edit, home, import, settings, timetable, todo. |

## Navigation

- Navigation uses Navigation 3 (`NavDisplay` + `rememberNavBackStack`) defined in root `Navigation.kt`.
- Nav keys: `Main`, `AddEditCourse(courseId?)`, `ImportCourses` (defined in `NavigationKeys.kt`).
- `MainScreen` handles bottom-bar tab switching internally via `MainScreenViewModel.currentTab` (0=Home, 1=Timetable, 2=Todo, 3=Settings).
- Deep screens (add/edit course, import) are pushed onto the nav back stack by `MainScreen` via `onItemClick` callback.

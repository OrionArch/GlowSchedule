<!-- Parent: ../AGENTS.md -->

# theme/

Material 3 theming: color palette, theme composable, and typography.

## Key Files

| File | Purpose |
|------|---------|
| `Color.kt` | Defines light and dark theme color constants, 8 Morandi pastel color pairs (background + matching text), `MorandiColors` hex list, and `getContrastingTextColor()` helper. |
| `Theme.kt` | `GlowScheduleTheme` composable. Builds `lightColorScheme` / `darkColorScheme` from Color.kt constants. Sets status bar and navigation bar colors via `SideEffect`. |
| `Type.kt` | Material 3 `Typography` instance. Only `bodyLarge` is explicitly customized; all other styles use Material 3 defaults. |

## Color System

- **Theme colors**: Muted sage-green primary palette with rose secondary and steel-blue tertiary.
- **Course card colors**: 8 Morandi pastel backgrounds with guaranteed-contrast text colors, defined as hex strings in `MorandiColors` list.
- `getContrastingTextColor(hex, isDarkTheme)` returns the matching text `Color` for a given course card background hex.

## Usage

All composables should use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` accessed via `GlowScheduleTheme { ... }` wrapper set in `MainActivity`.

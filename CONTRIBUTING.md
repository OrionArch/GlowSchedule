# Contributing to GlowSchedule

First off, thank you for considering contributing to GlowSchedule! It's people like you that make this project better.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How to Contribute

### Bug Reports

If you find a bug, please open an issue using the **Bug Report** template on [GitHub Issues](https://github.com/OrionArch/GlowSchedule/issues). Include:

- Steps to reproduce
- Expected behavior
- Actual behavior
- Device info (Android version, screen size)
- Screenshots or logs if applicable

### Feature Requests

We welcome feature ideas! Open an issue using the **Feature Request** template and describe:

- The problem or use case
- Your proposed solution
- Any alternatives you've considered

### Code Contributions

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/my-feature`)
3. Make your changes
4. Ensure tests pass and the build succeeds
5. Open a Pull Request against the `main` branch

## Development Setup

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK** 17
- **Android SDK** with compileSdk 36
- **Git**

### Clone and Build

```bash
git clone https://github.com/OrionArch/GlowSchedule.git
cd GlowSchedule
./gradlew assembleDebug
```

### Run

Open the project in Android Studio, select the **app** configuration, and run on an emulator or physical device.

## Project Structure

```
app/src/main/java/com/example/schday/
├── data/           # Database entities, DAOs, repositories
│   ├── dao/
│   └── entity/
├── parser/         # Schedule file parsing logic
├── scheduler/      # Alarm and notification scheduling
├── theme/          # Material 3 theme definitions
├── ui/
│   ├── main/       # Main activity and navigation
│   └── screens/
│       ├── home/      # Home screen
│       ├── timetable/  # Timetable display
│       ├── edit/       # Schedule editing
│       ├── todo/       # Todo/task management
│       ├── settings/   # App settings
│       └── import/     # Schedule import
├── utils/          # Shared utilities
└── widget/         # Home screen widgets
```

## Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- For Compose UI, follow the [Compose API Guidelines](https://androidx.tech/artifacts/compose.ui/ui/1.0.0/api/ui-guidelines/)
- Use **Material 3** theming components throughout
- Package naming: `com.example.schday.<feature>`
- Use meaningful variable and function names; avoid abbreviations

## Commit Conventions

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add weekly timetable view
fix: correct alarm scheduling for midnight events
docs: update API documentation
refactor: extract schedule parsing into separate module
test: add unit tests for parser
chore: update Gradle to 9.1
```

Common prefixes: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`

## Pull Request Process

1. Create your PR against the `main` branch
2. Fill in the PR template completely
3. Ensure `./gradlew assembleDebug` passes without errors
4. Ensure all existing tests pass (`./gradlew test`)
5. Link any related issue (e.g., `Closes #12`)
6. Be responsive to code review feedback

A maintainer will review your PR and merge it once approved.

## Reporting Security Vulnerabilities

Please do **not** open a public issue for security vulnerabilities. See [SECURITY.md](SECURITY.md) for responsible disclosure instructions.

## License

By contributing to GlowSchedule, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

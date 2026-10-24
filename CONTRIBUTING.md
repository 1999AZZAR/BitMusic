# Contributing to BitMusic

Thank you for contributing to BitMusic. Follow these guidelines to ensure an efficient development and review process.

## Proposing Contributions
- BitMusic is focused specifically on large-screen experiences (Linux Desktop & Android TV).
- Substantial features, refactors, or UI redesigns should normally be discussed before implementation. Open an issue on GitHub to outline your proposal.
- Straightforward bug fixes, documentation improvements, or minor corrections can be submitted directly as a Pull Request.

## Branch and Pull Request Workflow
- Fork the repository and create a descriptive branch from `main` (e.g., `fix/streaming-buffer`, `feature/desktop-shortcut`).
- Keep pull requests focused on a single change or bug fix. Avoid mixing unrelated refactors, cosmetic tweaks, or mass reformatting with functional changes.
- Provide a clear PR description explaining what was changed and why.
- Include relevant testing and validation details with your PR.
- Address review feedback promptly and keep discussions focused on technical merits.

## Development Setup
The project requires the following tools:
- **JDK**: Java Development Kit 21 (Eclipse Temurin 21 recommended for Desktop; JDK 17+ for Android TV).
- **Linux Audio Libraries (for Desktop on Linux)**: PulseAudio / PipeWire (`libpulse-dev`), ALSA (`libasound2-dev`), GTK3, GL, and CMake.
- **Android SDK (Optional, for Android TV)**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (only needed if building `:tv`).
- **Listen Together Backend (Optional)**: Go 1.22+ if developing or testing the party server (`backend/`).

## Build and Test Commands
Run Gradle commands from the repository root:

### Desktop (Linux / macOS / Windows)
Run application locally:
```bash
./gradlew :desktopApp:run
```

Run desktop unit tests:
```bash
./gradlew :desktopApp:test
```

### Android TV
Build Android TV Dev Debug APK:
```bash
./gradlew :tv:assembleDevDebug
```

## Code Quality
- Follow idiomatic Kotlin and Compose conventions.
- Maintain the project's existing architecture and separation of concerns (Desktop Compose UI, Android TV UI, playback services, and data repositories).
- Write unit tests for new logic, fixes, and edge cases.
- Contributors should run relevant tests and must not introduce new failures.
- Keep diffs focused and minimal. Avoid unnecessary third-party dependencies.

## Dependencies and Licensing
BitChord is licensed under the **GNU General Public License v3.0 (GPLv3)**.

All contributed code and dependencies must be strictly compatible with GPLv3. Avoid introducing external dependencies unless strictly necessary; any new dependency must be evaluated for necessity, binary size, and license compliance.

# Contributing to BitMusic

Guidelines for contributing to BitMusic, covering branch workflows, development setup, testing, and licensing.

## Proposing Changes
- BitMusic focuses exclusively on large displays: Linux Desktop, Android TV, Windows, and macOS.
- For non-trivial features, architectural refactors, or UI redesigns, open an issue to discuss requirements before submitting code.
- Bug fixes, documentation updates, and test additions can be submitted directly as Pull Requests.

## Branch and Development Workflow
BitMusic enforces a structured branch lifecycle:

| Branch | Purpose | Versioning Scheme | Rules |
|---|---|---|---|
| `master` | **Final Production Version** | `1.0.0` (clean semver `X.Y.Z`) | Protected branch representing release-ready code. Tags (`v*.*.*`) are cut from `master`. Changes arrive strictly via promotion from `staging`. |
| `staging` | **Ongoing Development & Integration** | `1.0.0-beta-0` (or `-beta-N`) | Active development branch where new features and bug fixes land and undergo testing before production release. |
| `feature/<name>` | **New Features & Improvements** | `1.0.0-beta-<name>-0` | Created off `staging`. Work is promoted via Pull Request (`PR -> review -> acceptance -> merge`) into `staging`. |
| `fix/<name>` | **Bug Fixes** | `1.0.0-beta-<name>-0` | Created off `staging`, promoted via Pull Request into `staging`. |

### Development Lifecycle
1. Branch off `staging`: `git checkout staging && git checkout -b feature/my-cool-feature`
2. Implement, test locally (`./gradlew :desktopApp:test`), and commit.
3. Open a Pull Request targeting `staging`.
4. Once reviewed and CI checks pass, merge into `staging`.
5. Production releases are promoted from `staging` to `master` and tagged.

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
BitMusic is licensed under the **GNU General Public License v3.0 (GPLv3)**.

All contributed code and dependencies must be strictly compatible with GPLv3. Avoid introducing external dependencies unless strictly necessary; any new dependency must be evaluated for necessity, binary size, and license compliance.

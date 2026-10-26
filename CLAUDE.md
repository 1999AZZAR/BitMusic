# Claude Code Instructions for BitMusic

BitMusic is a big-screen music application for Linux Desktop, Android TV, Windows, and macOS (GPLv3).

Detailed specifications, rules, and architecture are in [AGENTS.md](AGENTS.md) and [.cursor/rules/](.cursor/rules/).

## Essential Build & Test Commands

- **Run Desktop App**: `./gradlew :desktopApp:run`
- **Run Unit Tests**: `./gradlew :desktopApp:test`
- **Build Native Audio**: `./gradlew :desktopApp:buildAnalysisNative`
- **Build Android TV**: `./gradlew :tv:assembleDevDebug`
- **Local Linux Package & Install**: `./scripts/local-release.sh`

## Core Rules

1. **Big Screen Only**: Target only Linux Desktop, Android TV, Windows, macOS. Mobile phone support is decoupled and forbidden.
2. **Git Workflow**:
   - `master`: Production release-ready (`1.1.0`). Never commit directly.
   - `staging`: Ongoing development (`1.1.0-beta-0`, `-beta-1`).
   - `feature/<name>` / `fix/<name>`: Created off `staging`, promoted via Pull Request into `staging`.
3. **Validation**: Always run `./gradlew :desktopApp:test` before opening PRs.
4. **Locale Safety**: Ensure native C/C++ JSON formatting remains RFC 8259 compliant (`.` decimal point, never `,`).

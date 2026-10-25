# GitHub Copilot Instructions for BitMusic

BitMusic is a bloat-free, big-screen audio application for Linux Desktop, Android TV, Windows, and macOS.

See [AGENTS.md](../AGENTS.md) and [.cursor/rules/](../.cursor/rules/) for comprehensive project rules and guidelines.

## Key Directives for Copilot

- **Target Platforms**: Big screens only (Linux Desktop, Android TV Leanback, Windows, macOS). Mobile phone layouts are strictly excluded.
- **Git Strategy**:
  - `master`: Production release-ready code (`1.1.0`).
  - `staging`: Active integration and development (`1.1.0-beta-0`).
  - `feature/<name>` / `fix/<name>`: Created from `staging`, promoted back to `staging` via PR.
- **Verification**: Run `./gradlew :desktopApp:test` to validate all code modifications.
- **Audio Architecture**: Linux low-latency capture relies on PulseAudio / PipeWire monitor sources feeding native `cavacore`.
- **License**: GNU General Public License v3.0 (GPLv3).

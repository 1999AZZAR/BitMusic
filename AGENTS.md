# AGENTS.md — AI Coding Assistant Guidance for BitMusic

This document is the authoritative guide for AI coding agents (OpenCode, OpenAI Codex, Claude Code, Antigravity, Cursor, Windsurf, Zed, Copilot, etc.) working on BitMusic.

---

## 1. Project Mission & Big-Screen Focus

BitMusic is a bloat-free, high-performance music application crafted strictly for **Big-Screen devices**:
- **Linux Desktop**: Low-latency native audio capture via PulseAudio / PipeWire directly feeding the spectrum analyzer.
- **Windows Desktop**: WASAPI loopback capture, Compose Multiplatform desktop.
- **macOS Desktop**: CoreAudio capture, Compose Multiplatform desktop.

> ⚠️ **Strict Constraint — No Mobile Support**:
> Mobile phone / handheld support has been intentionally decoupled and removed. Do not introduce touchscreen-only gestures, phone layout wrappers, portrait locks, or mobile-specific workflows.

---

## 2. Git Branch & Versioning Lifecycle

All development must follow this strict branch lifecycle:

| Branch | Role | Version Pattern | Promotion Rules |
|---|---|---|---|
| `master` | **Final Production** | Clean `X.Y.Z` | Protected production branch. Tags (`v*.*.*`) are cut here. Direct commits forbidden; changes enter strictly via promotion from `staging`. |
| `staging` | **Ongoing Development** | `1.4.3-beta-0` (or `-beta-N`) | Integration branch where all new features and bug fixes land and undergo testing. |
| `feature/<name>` | **New Features** | `1.4.3-beta-<name>-0` | Branched from `staging`. Promoted via Pull Request (`PR -> review -> CI check -> merge`) into `staging`. |
| `fix/<name>` | **Bug Fixes** | `1.4.3-beta-<name>-0` | Branched from `staging`. Promoted via Pull Request into `staging`. |

### Versioning Rules
- The development base is `1.4.3`; staging starts at `1.4.3-beta-0`. Production remains at its last promoted release until staging is explicitly promoted.
- Release script [`scripts/local-release.sh`](scripts/local-release.sh) detects the active branch and automatically stamps the appropriate debian version.
- On `master`, builds produce the clean version recorded by the promoted `version.txt`.
- On `staging`, builds increment `1.4.3-beta-0`, `1.4.3-beta-1`, etc.
- On `feature/<name>`, builds produce `1.4.3-beta-<name>-0`.

---

## 3. Repository Architecture & Key Directories

- `:desktopApp` (`desktopApp/`) — JVM desktop entry point, native visualizer loader, packaging (deb, rpm, AppImage, exe, dmg).
- `:desktopUi` (`desktopUi/`) — Large viewport Compose Multiplatform UI, synchronized lyrics, frosted-glass panels, media controls.
> Note: Android phone (`:app`) and Android TV (`:tv`) modules have been removed. BitMusic is now desktop-only (Linux, Windows, macOS).
- `:shared` (`shared/`) — Core business logic, repositories, playback models, YouTube Music & Spotify connectors.
- `native/` — C/C++ audio analysis engine (`cavacore`), PulseAudio loopback capture, CMake scripts, JNI bindings.
- `backend/` — Optional Go 1.22+ Listen Together party synchronization server.
- `.cursor/rules/` — Detailed sub-rules for Cursor (`rules.mdc`, `git-workflow.mdc`, `desktop-audio.mdc`).

---

## 4. Audio Engine & Native Analysis Rules

- **Native Capture**:
  - Linux: PulseAudio / PipeWire monitor sources directly into `cavacore`.
  - Windows: WASAPI loopback.
  - macOS: CoreAudio.
- **Graceful Fallbacks**: If native capture devices or monitor sources are unavailable, fallback gracefully to software FFT without crashing.
- **JSON Serialization Invariant**:
  - All native C++ JNI formatting (`native/analyzer/jni/analysis_jni.cpp`) must use standard RFC 8259-compliant `.` decimal points regardless of system locale (e.g. `LC_NUMERIC=id_ID.UTF-8`). Never emit commas in floating-point JSON output.

---

## 5. Build, Test & Verification Commands

All terminal commands should be executed from the repository root:

### Desktop
- **Run Locally**:
  ```bash
  ./gradlew :desktopApp:run
  ```
- **Run Unit Tests** (Mandatory before opening PRs):
  ```bash
  ./gradlew :desktopApp:test
  ```
- **Build Native Analysis Library**:
  ```bash
  ./gradlew :desktopApp:buildAnalysisNative
  ```
- **Local Linux Release (.deb build & install)**:
  ```bash
  ./scripts/local-release.sh
  ```

### Desktop-only
BitMusic now targets Linux, Windows and macOS only; there is no Android build.

---

## 6. Coding Standards & Hygiene

- **Java / Kotlin**: JDK 21 runtime. Use idiomatic Kotlin, structured concurrency (coroutines), and Compose Multiplatform patterns.
- **Clean Code**: Self-documenting names, security-first validation, comprehensive error handling. Never leave placeholder comments (`// TODO do stuff later`) or unused dead code.
- **License**: GNU General Public License v3.0 (GPLv3). All contributions and external libraries must be strictly GPLv3 compatible.
- **Bloat-Free**: Favor lightweight, performant implementations. Avoid bloated dependencies.

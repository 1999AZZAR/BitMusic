# Specification: Enhanced Android TV Remote Navigation & 10-Foot Media Controls

## 1. Overview
Enhance the Android TV experience in `:tv` with responsive remote control inputs, glowing focus feedback, and frictionless 10-foot media controls. This track addresses D-pad navigation friction, adds hardware media key shortcuts (Play/Pause, Fast-Forward, Rewind, Next, Prev), implements a high-visibility glowing accent focus ring with subtle 1.05x scale bounce, and provides a quick-access slide-in queue and lyric drawer during full-screen playback.

## 2. Functional Requirements
- **FR-1: Remote Key Event Interception**:
  - Implement global handling in `TvActivity` for key events:
    - `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_PLAY`, `KEYCODE_MEDIA_PAUSE`
    - `KEYCODE_MEDIA_FAST_FORWARD` (seek forward 10s), `KEYCODE_MEDIA_REWIND` (seek backward 10s)
    - `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`
    - `KEYCODE_DPAD_UP` (when in playback view to reveal queue/lyric drawer)
    - `KEYCODE_MENU` (contextual track actions / toggle drawer)
- **FR-2: 10-Foot Visual Focus State**:
  - Add unified focus modifier for Leanback cards and interactive buttons:
    - Glowing accent border (theme-aware primary color).
    - Subtle 1.05x scale bounce with smooth spring animation.
    - Clear elevation shift on focus.
- **FR-3: Quick Slide-in Queue & Lyric Drawer**:
  - Provide a non-intrusive side-drawer overlay over full-screen playback.
  - Triggerable via `DPAD_UP` or `KEYCODE_MENU`.
  - Accessible via D-pad navigation without disrupting background audio or video playback.
  - D-pad left/right switches between Queue tab and Synchronized Lyrics tab.

## 3. Non-Functional Requirements
- **NFR-1: Low Latency & Frame Rate**:
  - D-pad focus transitions must maintain stable 60/120 FPS without UI jank.
- **NFR-2: Zero Touch Dependency**:
  - All navigation states must be 100% reachable via D-pad arrows, Enter, and Back.

## 4. Acceptance Criteria
- [ ] Hardware remote media keys trigger correct player actions without requiring transport controls to be focused.
- [ ] Focused cards in Home, Explore, and Library prominently highlight with glowing border and 1.05x spring scale.
- [ ] Pressing `DPAD_UP` during playback opens the slide-in drawer showing upcoming songs and synchronized lyrics.
- [ ] Unit tests for key mapping and state dispatch pass cleanly with `./gradlew :desktopApp:test` and TV compile checks pass with `./gradlew :tv:assembleDevDebug`.

## 5. Out of Scope
- Touch gestures or mobile phone controls (forbidden by project rules).
- Multi-user remote pairing or Bluetooth accessory management.

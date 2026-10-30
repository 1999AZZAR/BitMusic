# Implementation Plan: Enhanced Android TV Remote Navigation & 10-Foot Media Controls

## Phase 1: Key Event Handling & Remote Input Mapping (TDD)
- [x] Task: Write Key Event Mapping Tests
  - [x] Create TvRemoteKeyDispatcherTest for media keys, seek offsets, and drawer triggers
  - [x] Verify tests fail as expected (Red Phase)
- [x] Task: Implement Remote Key Interception in TvActivity
  - [x] Hook dispatchKeyEvent for MEDIA_PLAY_PAUSE, FAST_FORWARD, REWIND, NEXT, PREV
  - [x] Wire DPAD_UP and MENU to playback drawer state
  - [x] Verify all tests pass (Green Phase)
- [x] Task: Phase 1 Verification & Checkpoint (Refer to workflow.md)

## Phase 2: 10-Foot Focus State & Animation Modifiers (TDD)
- [x] Task: Write Focus State Unit Tests
  - [x] Test focus scale and border properties
  - [x] Verify tests fail as expected (Red Phase)
- [x] Task: Implement Focus Ring Modifier in TvKit
  - [x] Create tvFocusBorderModifier with 1.05x scale bounce & spring transition
  - [x] Apply modifier to cards in TvHomeScreen, TvExploreScreen, and TvLibraryScreen
  - [x] Verify tests pass (Green Phase)
- [x] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Slide-in Queue & Lyrics Drawer (TDD)
- [x] Task: Write Drawer State & Navigation Tests
  - [x] Test drawer open/close transitions and D-pad tab switching
  - [x] Verify tests fail as expected (Red Phase)
- [x] Task: Implement Slide-in Drawer Component
  - [x] Build TvNowPlayingDrawer composable with Queue and Lyrics tabs
  - [x] Integrate with TvNowPlayingScreen without interrupting background playback
  - [x] Verify tests pass (Green Phase)
- [x] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

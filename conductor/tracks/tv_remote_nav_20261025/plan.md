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
- [ ] Task: Write Focus State Unit Tests
  - [ ] Test focus scale and border properties
  - [ ] Verify tests fail as expected (Red Phase)
- [ ] Task: Implement Focus Ring Modifier in TvKit
  - [ ] Create tvFocusBorderModifier with 1.05x scale bounce & spring transition
  - [ ] Apply modifier to cards in TvHomeScreen, TvExploreScreen, and TvLibraryScreen
  - [ ] Verify tests pass (Green Phase)
- [ ] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Slide-in Queue & Lyrics Drawer (TDD)
- [ ] Task: Write Drawer State & Navigation Tests
  - [ ] Test drawer open/close transitions and D-pad tab switching
  - [ ] Verify tests fail as expected (Red Phase)
- [ ] Task: Implement Slide-in Drawer Component
  - [ ] Build TvNowPlayingDrawer composable with Queue and Lyrics tabs
  - [ ] Integrate with TvNowPlayingScreen without interrupting background playback
  - [ ] Verify tests pass (Green Phase)
- [ ] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

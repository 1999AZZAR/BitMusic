# Implementation Plan: Audio Quality Indicators on Now Playing UI

## Phase 1: Domain Models & Quality Tier Formatter (TDD)
- [ ] Task: Write AudioQualityBadgeFormatter Tests
  - [ ] Unit tests for 4 quality tiers (Hi-Res, Lossless, HQ, SQ) and stream spec strings
  - [ ] Verify tests fail as expected (Red Phase)
- [ ] Task: Implement AudioQualityTier & Stream Formatter in :shared
  - [ ] Create AudioQualityTier enum and formatStreamDetails helper
  - [ ] Verify all tests pass (Green Phase)
- [ ] Task: Phase 1 Verification & Checkpoint (Refer to workflow.md)

## Phase 2: Desktop Now Playing UI Quality Badge
- [ ] Task: Update Desktop PlayerControls & NowPlayingScreen
  - [ ] Add Standard Quality pill and codec/bitrate stream specs
  - [ ] Support shimmer on Lossless and breathing pulse on live upgrade race
  - [ ] Verify with tests and desktop compile
- [ ] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Android TV Now Playing UI Quality Badge
- [ ] Task: Update TvNowPlayingParts Quality Line
  - [ ] Render Standard, High Quality, and Lossless badges in 10-foot TV UI
  - [ ] Display codec & bitrate details inline
  - [ ] Verify TV compile checks
- [ ] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

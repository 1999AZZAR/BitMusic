# Implementation Plan: Audio Quality Indicators on Now Playing UI

## Phase 1: Domain Models & Quality Tier Formatter (TDD)
- [x] Task: Write AudioQualityBadgeFormatter Tests
  - [x] Unit tests for 4 quality tiers (Hi-Res, Lossless, HQ, SQ) and stream spec strings
  - [x] Verify tests fail as expected (Red Phase)
- [x] Task: Implement AudioQualityTier & Stream Formatter in :shared
  - [x] Create AudioQualityTier enum and formatStreamDetails helper
  - [x] Verify all tests pass (Green Phase)
- [x] Task: Phase 1 Verification & Checkpoint (Refer to workflow.md)

## Phase 2: Desktop Now Playing UI Quality Badge
- [x] Task: Update Desktop PlayerControls & NowPlayingScreen
  - [x] Add Standard Quality pill and codec/bitrate stream specs
  - [x] Support shimmer on Lossless and breathing pulse on live upgrade race
  - [x] Verify with tests and desktop compile
- [x] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Android TV Now Playing UI Quality Badge
- [x] Task: Update TvNowPlayingParts Quality Line
  - [x] Render Standard, High Quality, and Lossless badges in 10-foot TV UI
  - [x] Display codec & bitrate details inline
  - [x] Verify TV compile checks
- [x] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

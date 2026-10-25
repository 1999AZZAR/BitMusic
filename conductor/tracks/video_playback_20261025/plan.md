# Implementation Plan: Dedicated Desktop Video Playback & Now Playing Switch

## Phase 1: Video Stream Resolution & Data Models (TDD)
- [x] Task: Write Video Stream Resolution Tests
  - [x] Create unit tests for video stream resolution in `:shared`
  - [x] Test resolving 720p H.264 pre-muxed stream URLs
  - [x] Test graceful fallback to audio-only stream when video rendition is absent
  - [x] Verify tests fail as expected (Red Phase)
- [x] Task: Implement Video Stream Resolution in :shared & :desktopApp
  - [x] Add `resolveVideoStream(videoId: String)` in `StreamResolver.kt`
  - [x] Add `DesktopVideoStream` and format descriptors in `DesktopPlayback.kt`
  - [x] Verify all tests pass (Green Phase)
- [x] Task: Phase 1 Verification & Checkpoint (Refer to workflow.md)

## Phase 2: Video Player Engine & Playhead Synchronization
- [x] Task: Video Playback Surface & Decoding Pipeline
  - [x] Create hardware-accelerated Compose video surface adapter for desktop
  - [x] Implement playhead clock synchronization (A/V sync) between video presentation and audio output
  - [x] Add buffering and seek handling for windowed video stream reads
- [x] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Now Playing UI Integration (Song / Video Switch & Viewport)
- [x] Task: Implement Song / Video Toggle
  - [x] Add animated "Song / Video" mode switch pill in `DesktopNowPlaying.kt`
  - [x] Wire state preservation (retaining timestamp and play state during mode switches)
- [x] Task: Integrate 16:9 Video Viewport in Desktop Now Playing
  - [x] Render responsive 16:9 video container replacing artwork and spectrum visualizer when in Video mode
  - [x] Add double-click to toggle expanded theater presentation
  - [x] Maintain scrubber and transport controls below the video container
- [x] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

## Phase 4: Verification, Local Packaging & Release
- [x] Task: Full Regression Testing & Verification
  - [x] Run `:shared:jvmTest` and `:desktopApp:test`
  - [x] Verify zero audio pipeline degradation when Video mode is idle
- [x] Task: Package and Test Local Release
  - [x] Run `./scripts/local-release.sh` on feature branch
  - [x] Verify local installation and end-to-end playback

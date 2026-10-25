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
- [~] Task: Video Playback Surface & Decoding Pipeline
  - [ ] Create hardware-accelerated Compose video surface adapter for desktop
  - [ ] Implement playhead clock synchronization (A/V sync) between video presentation and audio output
  - [ ] Add buffering and seek handling for windowed video stream reads
- [ ] Task: Phase 2 Verification & Checkpoint (Refer to workflow.md)

## Phase 3: Now Playing UI Integration (Song / Video Switch & Viewport)
- [ ] Task: Implement Song / Video Toggle
  - [ ] Add animated "Song / Video" mode switch pill in `DesktopNowPlaying.kt`
  - [ ] Wire state preservation (retaining timestamp and play state during mode switches)
- [ ] Task: Integrate 16:9 Video Viewport in Desktop Now Playing
  - [ ] Render responsive 16:9 video container replacing artwork and spectrum visualizer when in Video mode
  - [ ] Add double-click to toggle expanded theater presentation
  - [ ] Maintain scrubber and transport controls below the video container
- [ ] Task: Phase 3 Verification & Checkpoint (Refer to workflow.md)

## Phase 4: Verification, Local Packaging & Release
- [ ] Task: Full Regression Testing & Verification
  - [ ] Run `:shared:jvmTest` and `:desktopApp:test`
  - [ ] Verify zero audio pipeline degradation when Video mode is idle
- [ ] Task: Package and Test Local Release
  - [ ] Run `./scripts/local-release.sh` on feature branch
  - [ ] Verify local installation and end-to-end playback

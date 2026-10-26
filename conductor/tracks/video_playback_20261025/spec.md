# Specification: Dedicated Desktop Video Playback & Now Playing Switch

## Overview
Adds music video and live concert playback capabilities to BitMusic on Desktop (Linux, Windows, macOS). Introduces an intuitive "Audio / Video" toggle switch on the Now Playing screen, replacing the artwork/spectrum visualizer with a hardware-accelerated 16:9 video surface while maintaining low resource usage and bloat-free performance.

---

## Functional Requirements

### 1. Video Stream Resolution
- Extend `StreamResolver` to resolve pre-muxed 720p HD video streams (H.264 + AAC, typically itag 22) for tracks having valid video IDs (`song.videoId`).
- Support seamless caching and URL expiration handling consistent with existing audio resolution.
- Fallback gracefully to standard audio stream (Opus 160k) if a video stream cannot be resolved or decoded.

### 2. Audio / Video Mode Toggle
- Add an interactive "Song / Video" pill toggle at the top of the Now Playing player deck.
- Toggle is enabled when the active track is recognized as a video or has video renditions available.
- Switching modes preserves playback state, position, and audio continuity without reloading the track from scratch when possible.

### 3. Video Viewport & UI Integration
- In Video mode:
  - Replace the album artwork container and `SpectrumPane` with a high-performance 16:9 video viewport.
  - Controls, scrubber, and timestamps remain accessible below the video surface.
  - Support double-click on the video canvas to toggle expanded theater / full-height presentation.
- In Audio mode:
  - The standard artwork, dynamic background mesh, and low-latency Cava audio spectrum remain default.

### 4. Transport & Playhead Synchronization
- Ensure transport controls (Play, Pause, Scrubbing, Volume) apply bidirectionally to video and audio tracks.
- Scrubber position updates seamlessly with video presentation timestamps (PTS).

---

## Non-Functional Requirements
- **Performance & Resource Safety**:
  - Memory footprint bounded with windowed streaming buffers (no full-file disk caching).
  - Hardware-accelerated video decoding (VA-API / D3D11 / VideoToolbox / native surface) to prevent CPU spikes.
- **Audio Integrity**: Audio quality standards (bitrate, sample rate) maintained during video playback.

---

## Acceptance Criteria
1. Resolves 720p H.264 video streams on-demand when user activates Video mode.
2. The Now Playing screen displays the video surface seamlessly within the layout.
3. Play, pause, seek, and volume controls operate deterministically with the video presentation.
4. Switching back to "Song" mode instantly restores the album artwork and Cava audio visualizer.
5. All automated unit and desktop tests pass without regressions.

---

## Out of Scope
- Android TV video implementation (deferred to dedicated TV video track).
- Adaptive 4K/1080p separate multi-track DASH stream muxing (Phase 2 enhancement).
- Mobile devices (excluded per product guidelines).

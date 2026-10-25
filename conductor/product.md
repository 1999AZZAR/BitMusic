# Product Definition: BitMusic

## Executive Summary
BitMusic is a bloat-free, high-performance music application built strictly for big-screen environments: Linux Desktop, Android TV, Windows, and macOS. It pairs low-latency native audio loopback capture (PulseAudio / PipeWire, WASAPI, CoreAudio) with an embedded C++ spectrum analysis engine (`cavacore`), synchronized syllable-level lyrics, and an Android TV Leanback remote-controlled 10-foot experience.

## Target Audience & Platforms
- **Primary Environments**:
  - **Linux Desktop**: Native PulseAudio and PipeWire monitor source loopback capture with hardware-accelerated Compose Multiplatform UI.
  - **Android TV / Google TV**: 10-foot television interface with full D-pad remote navigation, Leanback cards, QR-code authentication, and refresh rate matching.
  - **Windows & macOS Desktop**: WASAPI loopback capture and CoreAudio capture with Compose Multiplatform desktop packaging.
- **Explicit Anti-Target**: Mobile phones and handheld touchscreen devices. Mobile phone support has been intentionally decoupled and removed.

## Core Value Propositions
1. **Low-Latency Native Visualizer**: Direct native loopback audio capture feeding real-time FFT/Cava spectrum visualizations without audio pipeline degradation.
2. **Big-Screen Ergonomics**: Optimized for 1080p, 1440p, 4K monitors and television displays with keyboard shortcuts, media keys, and TV remote D-pad controls.
3. **Synchronized Syllable-Level Lyrics**: Sub-word synchronized lyric rendering with smooth animated syllable progression and translation support.
4. **Lightweight & Bloat-Free**: Fast cold-start, minimal runtime footprint, deterministic background threads, and strictly GPLv3-compatible licensing.
5. **Decoupled Architecture**: Modular architecture separating UI, business logic, native capture, and TV experiences.

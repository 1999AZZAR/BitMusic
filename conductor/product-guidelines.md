# Product Guidelines: BitMusic

## 1. Visual & UX Principles

### Big Screen & 10-Foot Ergonomics
- **Desktop (Monitors & Displays)**:
  - Clean layouts utilizing wide viewports effectively without sparse empty voids.
  - Frosted-glass translucent panels (`Haze`), subtle smooth animations, and high-contrast readable typography.
  - Full keyboard accessibility and global MPRIS media control support.
- **Android TV (10-Foot Experience)**:
  - Strict D-pad remote navigation with high-visibility focus rings and elevation states.
  - Zero requirement for mouse pointers or touch gestures.
  - QR-code authentication mechanisms to eliminate tedious on-screen text typing.
  - Refresh rate matching support (e.g. 120Hz switching) for silky playback and visualizer responsiveness.

## 2. Voice, Tone & Messaging
- **Technical & Direct**: Clear, concise, and bloat-free error messages and status feedback.
- **Professional & Uncluttered**: No promotional upsells, telemetry nags, or distracting banners.
- **Attribution & Open Source Respect**: Transparent upstream attribution to BitChord while proudly representing the independent BitMusic identity.

## 3. Engineering & Performance Guidelines
- **Real-Time Audio Safety**: All audio analysis, DSP, and spectrum calculations must run on dedicated background threads to prevent frame drops in the UI render loop.
- **Graceful Fallbacks**: Hardware acceleration, native capture sources, and specific monitor sinks must always degrade gracefully to safe software fallbacks if missing or restricted.
- **Memory & Resource Discipline**: Constant bounded cache sizes for album artwork, lyrics, and audio history to prevent memory leaks during long-running desktop or TV playback sessions.

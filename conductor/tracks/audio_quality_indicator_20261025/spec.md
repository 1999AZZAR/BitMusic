# Specification: Audio Quality Indicators on Now Playing UI

## 1. Overview
Introduce dynamic, real-time audio quality badges to the Now Playing screens on both Desktop UI and Android TV UI. BitMusic dynamically upgrades playback streams on the fly (e.g. starting with Opus and seamlessly swapping to FLAC/ALAC when a lossless source is acquired). This track surfaces the active decoding tier—Standard Quality (SQ), High Quality (HQ), Lossless, or Hi-Res Lossless—alongside codec, sample rate, bit depth, and bitrate metadata, giving users complete transparency into their audio fidelity.

## 2. Functional Requirements
- **FR-1: Comprehensive Tier Classification**:
  - Map `NerdStats.Snapshot` into clear visual tiers:
    - **Hi-Res Lossless**: `isHiRes == true` (FLAC/ALAC/WAV >16-bit or >48kHz). Gold/Amber accent pill with shimmer.
    - **Lossless**: `isLossless == true` (FLAC/ALAC/WAV 16-bit/44.1kHz). Cyan/Accent pill with shimmer.
    - **High Quality (HQ)**: `isHiQuality == true` (Bitrate >= 256 kbps, e.g. 320 kbps MP3/AAC). Frosted pill.
    - **Standard Quality (SQ)**: `isMediumQuality == true || (isLossless != true && isHiQuality != true && bitrate > 0)` (e.g. YouTube Music Opus ~160 kbps, AAC 128-192 kbps). Subtle translucent pill.
    - **Upgrading / Racing**: When `racingLossless` is active in background, display an animated "Upgrading to Lossless..." pulse.
- **FR-2: Detailed Stream Info Subtitle / Tooltip**:
  - Show precise stream metrics alongside the tier badge:
    - Format pattern: `<Codec> · <Bitrate> kbps · <BitDepth>-bit / <SampleRate> kHz`
    - Examples:
      - `Standard Quality` · `Opus · 160 kbps · 48 kHz`
      - `High Quality` · `AAC · 320 kbps · 44.1 kHz`
      - `Lossless` · `FLAC · 16-bit / 44.1 kHz`
      - `Hi-Res Lossless` · `FLAC · 24-bit / 96 kHz`
- **FR-3: Now Playing Screen Integration**:
  - **Desktop UI**: Display below the song title/artist in `NowPlayingScreen.kt` and `PlayerControls.kt`. Click/hover displays stream details.
  - **Android TV UI**: Display within `TvNowPlayingParts.kt` (`TvQualityLine`) so 10-foot viewers see the badge and stream specs clearly without opening dense debug menus.

## 3. Non-Functional Requirements
- **NFR-1: Zero Audio Overhead**: Reads from existing `NerdStats.current` StateFlow without extra audio decoding passes or I/O latency.
- **NFR-2: Dynamic Transitions**: Immediately updates badge visuals when on-the-fly lossless upgrade completes.

## 4. Acceptance Criteria
- [ ] Now Playing UI displays "Standard Quality" for default YouTube Music Opus streams.
- [ ] High-bitrate lossy streams display "High Quality" on both Desktop and TV.
- [ ] Lossless and Hi-Res streams display their respective shimmer badges.
- [ ] Badge displays accurate codec, bitrate, and sample rate format subtitle.
- [ ] Unit tests for quality tier mapping pass with `./gradlew :desktopApp:test` and `:shared:jvmTest`.

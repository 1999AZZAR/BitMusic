# Technology Stack: BitMusic

## Core Languages & Runtimes
- **Kotlin 2.3.20**: Primary language across `:desktopApp`, `:desktopUi`, `:tv`, and `:shared` modules. Utilizes Kotlin Coroutines and Kotlinx Serialization.
- **Java 21 (JDK 21 / Eclipse Temurin)**: Runtime baseline for desktop JVM execution and Media3 compatibility.
- **C/C++ (C++17)**: Native audio analysis engine (`cavacore`), windowed-sinc anti-aliasing resampler, PulseAudio loopback monitor capture, and JNI bridges.
- **Go 1.22+**: Lightweight backend service for Listen Together party synchronization (`backend/`).

## Frameworks & UI Architecture
- **Compose Multiplatform (Desktop)**: Hardware-accelerated UI rendering with `Haze` frosted-glass effects, animated lyrics canvas, and wide viewport responsiveness.
- **Android TV Leanback**: TV UI using Leanback catalog cards, D-pad remote focus trees, and QR code login components.
- **Media3 / ExoPlayer**: Audio playback foundation on Android with custom FFmpeg decoders.

## Audio Processing & Analysis
- **PulseAudio & PipeWire Native Capture**: Low-latency monitor source loopback capture on Linux.
- **WASAPI & CoreAudio**: Loopback capture on Windows and macOS.
- **cavacore**: C++ spectrum analysis and FFT visualizer engine.
- **ONNX Runtime (Int8)**: Neural beat tracking (`beat_this_int8.onnx`) and vocal separation (`vocals_umxhq_int8.onnx`).

## Build Systems & Tooling
- **Gradle 8.11.1**: Multi-project build tool with Kotlin DSL (`*.gradle.kts`).
- **Android Gradle Plugin 8.10.1**: Android SDK `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`.
- **CMake**: Native build orchestration for C++ analysis libraries.
- **Compose Desktop Packager**: Builds `.deb`, `.rpm`, `.AppImage`, `.exe`, `.msi`, `.dmg`, `.pkg`.
- **Packaging Helper**: `scripts/local-release.sh` with branch-aware version stamping.

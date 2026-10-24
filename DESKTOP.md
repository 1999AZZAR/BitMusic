# BitMusic Desktop

BitMusic Desktop is built with Compose Multiplatform on the JVM target, running on Linux, macOS, and Windows.

---

## Prerequisites

- **Java Development Kit**: JDK 21 (Eclipse Temurin 21 recommended).
- **Linux Build Dependencies**:
  - Audio: PulseAudio or PipeWire headers (`libpulse-dev` / `pipewire-pulse`), ALSA (`libasound2-dev`).
  - Graphics & UI: GTK 3 (`libgtk-3-dev`), GL (`libgl1-mesa-dev`), Pango, Cairo.
  - Native Toolchain: CMake 3.22+, `gcc`/`clang`, `binutils`.

---

## Running Locally

### Linux
Ensure system audio libraries and JDK 21 are on `PATH`, then run:
```bash
./gradlew :desktopApp:run
```

### macOS (Apple Silicon & Intel)
```bash
./gradlew :desktopApp:run
```

### Windows
From PowerShell or Command Prompt with JDK 21:
```bat
gradlew.bat :desktopApp:run
```

### NixOS
```bash
nix-shell -p jdk21 libglvnd glib gtk3 pango atk cairo cmake gdk-pixbuf libXtst libXxf86vm alsa-lib libpulse ffmpeg_6 --run '\
  export LD_LIBRARY_PATH="$(nix eval --raw nixpkgs#libglvnd.outPath)/lib:$(nix eval --raw nixpkgs#glib.out)/lib:$(nix eval --raw nixpkgs#gtk3.outPath)/lib:$(nix eval --raw nixpkgs#pango.out)/lib:$(nix eval --raw nixpkgs#atk.outPath)/lib:$(nix eval --raw nixpkgs#cairo.outPath)/lib:$(nix eval --raw nixpkgs#gdk-pixbuf.outPath)/lib:$(nix eval --raw nixpkgs#libXtst.outPath)/lib:$(nix eval --raw nixpkgs#libXxf86vm.outPath)/lib:$(nix eval --raw nixpkgs#alsa-lib.outPath)/lib:$(nix eval --raw nixpkgs#libpulse.outPath)/lib:$(nix eval --raw nixpkgs#ffmpeg_6.lib)/lib:$HOME/.openjfx/cache/21.0.6+3/amd64:/run/opengl-driver/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"; \
  bash ./gradlew :desktopApp:run
'
```

---

## Running Unit Tests

Run the desktop test suite:
```bash
./gradlew :desktopApp:test
```

To run only the audio spectrum test:
```bash
./gradlew :desktopApp:test --tests "com.music.bitchord.desktop.DesktopAudioSpectrumTest"
```

---

## Release Packaging

Pre-packaged desktop builds bundle their own Java runtime, native FFmpeg libraries, and ONNX models via `jpackage`.

| Platform | Format | Installation Command |
|---|---|---|
| **Linux** | `.AppImage` | `chmod +x BitMusic-*.AppImage && ./BitMusic-*.AppImage` |
| **Linux** | `.deb` | `sudo dpkg -i BitMusic-*.deb` or `sudo apt install ./BitMusic-*.deb` |
| **Linux** | `.rpm` | `sudo dnf install ./BitMusic-*.rpm` |
| **macOS** | `.dmg` | Open disk image, drag `BitMusic.app` to `/Applications` |
| **macOS** | `.pkg` | Standard macOS package installer |
| **Windows** | `.exe` | User-level installer (no administrator privileges required) |
| **Windows** | `.msi` | System-wide MSI installer |
| **Windows** | `.zip` | Portable archive; unpack and run `BitMusic.exe` |

---

## Building Packages from Source

Each platform-specific package is built on its target operating system:

### Linux
Requirements: `rpm`, `fakeroot`, `binutils`, and `file` (for AppImage):
```bash
# Build Debian package (.deb)
./gradlew :desktopApp:packageDeb

# Build Red Hat package (.rpm)
./gradlew :desktopApp:packageRpm

# Build AppImage
./gradlew :desktopApp:createDistributable
desktopApp/packaging/appimage.sh \
  desktopApp/build/compose/binaries/main/app/BitMusic \
  dist/BitMusic-linux-x86_64.AppImage
```

For quick local builds and immediate installation on Debian/Ubuntu/Mint:
```bash
./scripts/local-release.sh
```

### macOS
```bash
# Build .dmg disk image
./gradlew :desktopApp:packageDmg

# Build .pkg installer
./gradlew :desktopApp:packagePkg

# Build standalone .app bundle
./gradlew :desktopApp:createDistributable
# Output: desktopApp/build/compose/binaries/main/app/BitMusic.app
```

### Windows
Requirements: [WiX Toolset 3](https://github.com/wixtoolset/wix3/releases) on `PATH`:
```bat
REM Build EXE installer
gradlew.bat :desktopApp:packageExe

REM Build MSI package
gradlew.bat :desktopApp:packageMsi

REM Build portable archive
gradlew.bat :desktopApp:createDistributable
```

---

## Troubleshooting

### macOS Gatekeeper
Self-built or unsigned binaries on macOS may trigger a quarantine alert. Clear the quarantine attribute with:
```bash
xattr -cr /Applications/BitMusic.app
```

### Linux Graphics and Audio Dependencies
Packaged `.deb` and `.rpm` files bundle the runtime and audio decoders, but rely on desktop system graphics libraries (GTK 3, X11 or Wayland, GL, ALSA). Minimal server environments without an active display session will fail at startup.

### Windows Visual C++ Redistributable
The native audio analyzer links against a static C runtime. If Automix analysis fails to initialize on Windows, install the [Microsoft Visual C++ 2015–2022 Redistributable (x64)](https://aka.ms/vs/17/release/vc_redist.x64.exe).

# BitMusic TV

A dedicated, remote-first Android TV and Google TV interface for BitMusic. Built with Compose for TV (`androidx.tv.material3`), designed specifically for the 10-foot television experience.

---

## Highlights

- **Leanback Remote Navigation**: Full D-pad directional control, glowing focus states, and instant remote button mappings.
- **Fast Mobile Sign-In**: Scan a QR code on your TV screen to sign in with your phone or use the local web remote controller (`TvAuthServer`).
- **Synchronized Lyrics**: Full-screen Apple-style word-by-word animated lyrics rendered on 4K/1080p displays.
- **Display Refresh-Rate Switching**: Automatically matches TV panel refresh rate (24Hz, 50Hz, 60Hz, 120Hz) to prevent stutter.
- **Spatial Audio & Equalizer**: TV-optimized audio profile engine with room acoustics and surround expansion.

---

## Architecture

This module implements the TV interface and Leanback manifest while leveraging the shared audio engine:
- `src/main/java/.../ui/tv/**`: The Compose for TV interface (Home, Explore, Search, Library, Settings, Now Playing).
- `src/main/java/.../TvActivity.kt`: Leanback entry point.
- `src/main/res`: TV assets (banners, TV themes, launcher icons).
- Shared playback and data layers are linked from `:shared` and `:sharedUi`.

---

## Building & Installing

### Dev Build (Debug)
```bash
./gradlew :tv:assembleDevDebug
```
Output: `tv/build/outputs/apk/dev/debug/`

### Production Release (R8 Optimized)
```bash
./gradlew :tv:assembleProdRelease
```
Output: `tv/build/outputs/apk/prod/release/`

### Sideloading via ADB
To install directly to a connected Android TV or TV Box:
```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r tv/build/outputs/apk/prod/release/BitMusic-tv-prod-universal-release.apk
```

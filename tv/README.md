# BitMusic TV

BitMusic TV is a dedicated television interface built with Compose for TV (`androidx.tv.material3`), designed for Android TV and Google TV devices.

---

## Technical Specifications

- **Target Platforms**: Android TV / Google TV (minSdk 26, targetSdk 36, compileSdk 37).
- **Navigation Model**: Hardware D-pad directional controls (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER`, `BACK`).
- **Display Refresh Rate**: Matches display refresh rates (24Hz, 50Hz, 60Hz, 120Hz) to prevent video judder.
- **Authentication**: QR code phone sign-in via local embedded authentication server (`TvAuthServer`) to avoid on-screen keyboard typing.
- **Audio Output**: Uses shared playback engine with spatial expansion and TV equalizer profiles.

---

## Module Layout

- `src/main/java/com/music/bitchord/tv/ui/**`: Compose for TV UI screens (Home, Explore, Search, Library, Settings, Now Playing).
- `src/main/java/com/music/bitchord/tv/TvActivity.kt`: Android TV Leanback entry point and lifecycle handler.
- `src/main/res/**`: TV launcher assets, Leanback banner, and TV theme resources.
- Core business logic, data models, and network repositories are shared from `:shared`.

---

## Building and Installation

### Prerequisites
- Android SDK with Platforms 36/37 and Build Tools installed.
- Android TV device with ADB debugging enabled over Wi-Fi or USB.

### Build Debug APK
```bash
./gradlew :tv:assembleDevDebug
```
Output: `tv/build/outputs/apk/dev/debug/BitMusic-tv-dev-debug.apk`

### Build Production Release APK
```bash
./gradlew :tv:assembleProdRelease
```
Output: `tv/build/outputs/apk/prod/release/BitMusic-tv-prod-universal-release.apk`

### Sideloading via ADB
To install directly to an Android TV on the local network:
```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r tv/build/outputs/apk/prod/release/BitMusic-tv-prod-universal-release.apk
```

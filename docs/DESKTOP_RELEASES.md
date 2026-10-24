# Desktop & Big Screen Releases Protocol

How BitMusic releases are versioned, named, and distributed across platforms (Linux, Windows, macOS, and Android TV).

---

## 1. Versioning Specification

- The release version is a clean semver string such as `1.8` or `1.8.0` (or `1.8-beta1` for pre-releases).
- `desktopApp/build.gradle.kts` accepts it via `-Pbitchord.version=...` (falling back to `appVersion`). CI extracts the version directly from the git tag (`v1.8.0` -> `1.8.0`).
- The desktop runtime reads `System.getProperty("bitchord.version")`.
- The installer package version (`nativePackageVersion`) converts to a strict numeric format: `major.minor.desktopVersionCode` (e.g. `1.8.26`). Bump `desktopVersionCode` on every release to ensure proper in-place upgrades.

---

## 2. Release Artifacts Naming Convention

Every GitHub Release attached to tag `v<version>` packages the following canonical artifacts:

| Platform | Artifact | Target / Role |
|---|---|---|
| **Linux Desktop** | `BitMusic-<v>-linux-x86_64.AppImage` | Standalone zero-install executable (all distros) |
| **Linux Desktop** | `BitMusic-<v>-linux-amd64.deb` | Debian, Ubuntu, Linux Mint, Pop!_OS |
| **Linux Desktop** | `BitMusic-<v>-linux-x86_64.rpm` | Fedora, RHEL, openSUSE |
| **Android TV** | `BitMusic-tv-prod-universal-release.apk` | Sideloadable Leanback APK for Google TV & Android TV |
| **Windows Desktop** | `BitMusic-<v>-windows-x64-setup.exe` | Standard user installer (no admin required) |
| **Windows Desktop** | `BitMusic-<v>-windows-x64.msi` | Managed enterprise MSI package |
| **Windows Desktop** | `BitMusic-<v>-windows-x64-portable.zip` | Portable zero-install directory |
| **macOS Desktop** | `BitMusic-<v>-macos-arm64.dmg` | Apple Silicon (M1/M2/M3/M4) |
| **macOS Desktop** | `BitMusic-<v>-macos-x64.dmg` | Intel Macs |

---

## 3. Update Verification

- Update API endpoint: `https://api.github.com/repos/1999AZZAR/BitMusic/releases/latest`
- Sideloaded Linux deb/rpm packages upgrade cleanly via package manager (`sudo apt install ./BitMusic-*.deb` / `sudo dnf upgrade ./BitMusic-*.rpm`).
- Windows in-place upgrades rely on `upgradeUuid` (`8f3c1d24-6a2b-4f5e-9c17-2b8d54e0a913`).

---

## 4. Local Build Commands

```bash
# Linux Desktop distributable & packages
./gradlew :desktopApp:packageDeb :desktopApp:packageRpm :desktopApp:createDistributable

# Android TV Release APK
./gradlew :tv:assembleProdRelease
```

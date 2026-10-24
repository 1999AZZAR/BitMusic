# Desktop and TV Releases Protocol

This document defines how BitMusic builds, versions, and packages release artifacts for Linux, Android TV, Windows, and macOS.

---

## 1. Branch-Aware Versioning

BitMusic uses semantic versioning based on `1.1.0`:

- **Production (`master`)**: Strict semver string (`1.1.0`, `1.1.1`, `1.2.0`). Production release tags are named `v<version>` (e.g. `v1.1.0`).
- **Development (`staging`)**: Pre-release versions (`1.1.0-beta-0`, `1.1.0-beta-1`).
- **Feature Branches (`feature/<name>`)**: Feature pre-releases (`1.1.0-beta-<name>-0`).

The Gradle build resolves the version from the active git branch automatically or reads an explicit value passed via `-Pbitchord.version=<version>`.

---

## 2. Release Artifacts

Every release build produces the following canonical binaries:

| Platform | File Format | Build Command | Output Path |
|---|---|---|---|
| **Linux Desktop** | `.AppImage` | `./gradlew :desktopApp:createDistributable` + `appimage.sh` | `dist/BitMusic-<version>-linux-x86_64.AppImage` |
| **Linux Desktop** | `.deb` | `./gradlew :desktopApp:packageDeb` | `desktopApp/build/compose/binaries/main/deb/` |
| **Linux Desktop** | `.rpm` | `./gradlew :desktopApp:packageRpm` | `desktopApp/build/compose/binaries/main/rpm/` |
| **Android TV** | `.apk` | `./gradlew :tv:assembleProdRelease` | `tv/build/outputs/apk/prod/release/` |
| **Windows Desktop** | `.exe` | `gradlew.bat :desktopApp:packageExe` | `desktopApp/build/compose/binaries/main/exe/` |
| **Windows Desktop** | `.msi` | `gradlew.bat :desktopApp:packageMsi` | `desktopApp/build/compose/binaries/main/msi/` |
| **Windows Desktop** | `.zip` | `gradlew.bat :desktopApp:createDistributable` | `desktopApp/build/compose/binaries/main/app/` (zipped) |
| **macOS Desktop** | `.dmg` | `./gradlew :desktopApp:packageDmg` | `desktopApp/build/compose/binaries/main/dmg/` |
| **macOS Desktop** | `.pkg` | `./gradlew :desktopApp:packagePkg` | `desktopApp/build/compose/binaries/main/pkg/` |

---

## 3. Local Release Loop

On Debian, Ubuntu, and Linux Mint, use the local release helper to build, stamp, and install the current working tree directly:

```bash
./scripts/local-release.sh
```

The script determines the active branch, builds the `.deb`, updates package control metadata with the version stamp, and runs `sudo dpkg -i`.

---

## 4. GitHub Actions Release Workflow

Release generation is automated via `.github/workflows/release.yml`. When a tag matching `v*.*.*` is pushed to `master`:

1. Linux runner builds the `.deb`, `.rpm`, and `.AppImage`.
2. Windows runner builds the `.exe`, `.msi`, and portable `.zip`.
3. macOS runner builds the Apple Silicon and Intel `.dmg` and `.pkg`.
4. Android runner builds the production Leanback `.apk`.
5. GitHub Actions uploads all artifacts to the new GitHub Release.

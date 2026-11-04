# Desktop Releases Protocol

This document defines how BitMusic builds, versions, and packages release artifacts for Linux, Windows, and macOS.

---

## 1. Branch-Aware Versioning

BitMusic reads its semantic version base from `version.txt`:

- **Production (`master`)**: Clean `X.Y.Z` versions, such as `1.4.0`. Production release tags are named `v<version>`, such as `v1.4.0`.
- **Development (`staging`)**: Pre-release versions, such as `1.4.0-beta-0` and `1.4.0-beta-1`.
- **Feature/Fix Branches (`feature/<name>`, `fix/<name>`)**: Named pre-releases, such as `1.4.0-beta-<name>-0`.

The Gradle build resolves the version from the active git branch automatically or reads an explicit value passed via `-Pbitchord.version=<version>`.

---

## 2. Release Artifacts

Every release build produces the following canonical binaries:

| Platform | File Format | Build Command | Output Path |
|---|---|---|---|
| **Linux Desktop** | `.AppImage` | `./gradlew :desktopApp:createDistributable` + `appimage.sh` | `dist/BitMusic-<version>-linux-x86_64.AppImage` |
| **Linux Desktop** | `.deb` | `./gradlew :desktopApp:packageDeb` | `desktopApp/build/compose/binaries/main/deb/` |
| **Linux Desktop** | `.rpm` | `./gradlew :desktopApp:packageRpm` | `desktopApp/build/compose/binaries/main/rpm/` |
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

For Nix-built local packages, this helper also injects the Linux launcher wrapper that exposes host graphics libraries. Do not install the raw `packageDeb` output directly: it can fail to load `libGL.so.1`. To reuse an existing package, run `desktopApp/packaging/linux-stamp-deb.sh <input.deb> <output.deb> <version>` before installing the output.

After installation, verify the package version and open `/usr/share/applications/bitmusic.desktop` through the desktop launcher. An already-running process keeps its old version until it is restarted.

---

## 4. GitHub Actions Release Workflow

Release generation is automated via `.github/workflows/release.yml`. When code is merged/promoted to `master` (or when a tag matching `v*.*.*` is pushed, or via `workflow_dispatch`):

1. The `prepare` job resolves the release version from `version.txt` (or tag / input).
2. Linux runner builds the `.deb`, `.rpm`, and `.AppImage`.
3. Windows runner builds the `.exe`, `.msi`, and portable `.zip`.
4. macOS runner builds the Apple Silicon (`arm64`) `.dmg`, `.pkg`, and `.zip` (Intel `x64` is omitted from automated releases to prevent runner queue starvation).
5. GitHub Actions automatically generates release notes, tags the commit, and publishes the GitHub Release with all nine desktop distributables attached.

For a staging-to-master promotion, push `master` and let that workflow publish the release tag. Pushing an additional tag for the same promotion starts a second release build. Fetch the published tag afterward and verify that it resolves to the promoted master commit.

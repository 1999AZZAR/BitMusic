// Top-level build file where you can add configuration options common to all sub-projects/modules.
/*
 * The version of the phone app and of the watch app that ships inside it, in
 * one place. The watch has to move with the phone: it is installed and updated
 * from the phone's own APK (see app/build.gradle.kts), and Android refuses an
 * update whose versionCode is lower than the one installed.
 *
 * No "v" in the name — the in-app updater reads it as a number; the "v" is the
 * release tag's alone.
 */
extra["bitchordVersionCode"] = 10
extra["bitchordVersionName"] = "1.3.3"

/*
 * Bump this before cutting each test build ("beta0", "beta1", ...)
 * and blank it out before cutting the real production release.
 */
extra["bitchordBetaSuffix"] = "-beta-0"

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
}

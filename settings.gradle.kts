pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor is published via JitPack.
        maven("https://jitpack.io")
    }
}

rootProject.name = "BitMusic"
// Desktop-only application: Android phone (app) and Android TV (tv) modules
// have been removed. BitMusic now targets Linux, Windows and macOS only.
include(":shared")
include(":sharedUi")
include(":desktopUi")
include(":desktopApp")

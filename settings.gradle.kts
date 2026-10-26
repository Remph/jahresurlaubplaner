pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "urlaubsplaner"

include(":core")
// The Android app is skipped with -PcoreOnly, so :core builds and tests where the Android SDK
// and Google Maven are not reachable. Never apply Android plugins in the root build file.
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")

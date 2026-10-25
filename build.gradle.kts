// No Android plugins here: -PcoreOnly must be able to skip them (see settings.gradle.kts).
plugins {
    alias(libs.plugins.spotless)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

spotless {
    kotlin {
        target("*/src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint()
    }
}

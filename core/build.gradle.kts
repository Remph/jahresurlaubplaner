plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Bytecode level 17 so any JDK from 17 up builds it; the cloud container uses JDK 21.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// The tests read the real school holiday file; the app bundles the same directory as assets.
sourceSets {
    test {
        resources.srcDir(rootProject.file("data/school-holidays"))
    }
}

dependencies {
    // Only the JSON tree API for the backup file; no compiler plugin, so the format is written
    // out by hand and every bad value gets its own message.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

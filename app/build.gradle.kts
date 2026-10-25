plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "de.remph.urlaubsplaner"
    compileSdk = 37

    defaultConfig {
        // The application ID is permanent, do not change it.
        applicationId = "de.remph.urlaubsplaner"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // The school holiday file with its ODbL text and README (not in app/, shared with :core's
        // tests). Everything in that directory ends up in the APK, so the licence travels with it.
        getByName("main").assets.directories.add("../data/school-holidays")
        // The migration tests read the committed schemas as assets. Robolectric takes its assets
        // from the debug build, not from the test source set; the release build stays without them.
        getByName("debug").assets.directories.add("schemas")
    }

    testOptions {
        // Robolectric needs the merged resources and manifest to start a fake Android.
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    // Committed, so every schema change is reviewed and the migration tests can open old versions.
    schemaDirectory("$projectDir/schemas")
}

// Robolectric downloads its Android image from Maven Central on first use. Where Central is
// not reachable directly (the cloud container), pass a mirror: -ProbolectricRepoUrl=<url>.
tasks.withType<Test>().configureEach {
    providers.gradleProperty("robolectricRepoUrl").orNull?.let {
        systemProperty("robolectric.dependency.repo.url", it)
        systemProperty("robolectric.dependency.repo.id", "mirror")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    // AppCompatActivity and AppCompatDelegate carry the in-app language (Android 11 and up).
    implementation(libs.androidx.appcompat)
    // A few standard icons (back arrow, navigation bar); the core set only, not the big one.
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Room and Robolectric run on JUnit 4; :core keeps JUnit Jupiter.
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    // Compose tests on the JVM through Robolectric: the screens compose, show the right texts.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

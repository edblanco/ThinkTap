import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.dosparta.core.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    // Exposes the screenshot-test harness to other modules so its determinism settings
    // (SDK level, device qualifiers, reduced motion) are defined exactly once.
    testFixtures {
        enable = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Screenshot goldens are committed, so they must live outside `build/`, which `clean` wipes
// and `.gitignore` excludes.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

// Screenshot regressions should fail the build like any other test, matching the repo's
// fail-fast stance for Detekt and Lint.
tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    api(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.junit)
    testFixturesApi(libs.robolectric)
    testFixturesApi(libs.ui.test.junit4)
    testFixturesApi(libs.roborazzi)
    testFixturesApi(libs.roborazzi.compose)

    debugImplementation(libs.ui.test.manifest)
}

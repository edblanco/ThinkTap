import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.android.hilt)
    alias(libs.plugins.roborazzi)
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.dosparta.trivia.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    val baselineDirectory = if (System.getProperty("os.name") == "Linux") {
        "src/test/screenshots/linux"
    } else {
        "src/test/screenshots"
    }
    outputDir.set(file(baselineDirectory))
}

// Screenshot regressions should fail the build like any other test, matching the repo's
// fail-fast stance for Detekt and Lint.
tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":trivia-domain"))
    implementation(project(":data-trivia"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.hilt.navigation)
    implementation(libs.android.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ui.test.junit4)
    testImplementation(testFixtures(project(":core-ui")))
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)

    kspTest(libs.hilt.android.compiler)
    kspAndroidTest(libs.hilt.android.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
}
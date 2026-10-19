import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.android.hilt)
}

android {
    namespace = "com.dosparta.triviagame2"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dosparta.triviagame2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "tier"

    productFlavors {
        create("free") {
            dimension = "tier"
            buildConfigField("boolean", "SUPPORTS_EXTRA_LANGUAGES", "false")
            buildConfigField("boolean", "TRANSLATION_FRAMEWORKS_ENABLED", "false")
        }
        create("pro") {
            dimension = "tier"
            buildConfigField("boolean", "SUPPORTS_EXTRA_LANGUAGES", "true")
            buildConfigField("boolean", "TRANSLATION_FRAMEWORKS_ENABLED", "true")
            resValue("string", "firebase_api_key", providers.gradleProperty("thinktap.firebase.apiKey").getOrElse(""))
            resValue("string", "firebase_application_id", providers.gradleProperty("thinktap.firebase.applicationId").getOrElse(""))
            resValue("string", "firebase_project_id", providers.gradleProperty("thinktap.firebase.projectId").getOrElse(""))
            resValue("string", "google_web_client_id", providers.gradleProperty("thinktap.auth.webClientId").getOrElse(""))
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    androidResources {
        // Publishes the supported languages to Android 13+ per-app language settings.
        generateLocaleConfig = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
}

// Anchor task so CI running `./gradlew connectedDebugAndroidTest` tests all debug variants.
tasks.register("connectedDebugAndroidTest") {
    description = "Anchor task running connected Android tests across debug flavor variants"
    group = "verification"
    dependsOn(tasks.matching {
        it.name.startsWith("connected") && it.name.endsWith("DebugAndroidTest") && it.name != "connectedDebugAndroidTest"
    })
}

tasks.named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
    source("src/free/java", "src/pro/java", "src/testPro/java")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":feature-trivia-ui"))

    "freeImplementation"(libs.trivia.sdk.android) {
        exclude(group = "com.google.mlkit", module = "translate")
    }
    "proImplementation"(libs.trivia.sdk.android)
    "proImplementation"(platform(libs.firebase.bom))
    "proImplementation"(libs.firebase.auth)
    "proImplementation"(libs.androidx.credentials)
    "proImplementation"(libs.androidx.credentials.play.services)
    "proImplementation"(libs.google.id)
    "proImplementation"(libs.kotlinx.coroutines.play.services)
    "proImplementation"(libs.androidx.lifecycle.runtime.compose)
    "proImplementation"(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.android.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    "testProImplementation"(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

androidComponents {
    onVariants(selector().withFlavor("tier" to "free")) { variant ->
        variant.androidResources.localeFilters.set(setOf("en"))
    }
    onVariants(selector().withFlavor("tier" to "pro")) { variant ->
        variant.androidResources.localeFilters.set(setOf("en", "de", "es", "zh-rCN"))
    }
}

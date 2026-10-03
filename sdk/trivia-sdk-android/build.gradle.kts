import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.devtools.ksp)
    `maven-publish`
}

android {
    namespace = "com.dosparta.trivia.data"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":trivia-sdk-core"))
    implementation(project(":trivia-sdk-network"))

    implementation(libs.squareup.moshi)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.mlkit.translate)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    repositories {
        maven {
            name = "sdk"
            url = uri(
                providers.gradleProperty("sdkRepository").getOrElse(
                    rootProject.layout.buildDirectory.dir("repository").get().asFile.absolutePath
                )
            )
        }
    }
    publications {
        register<MavenPublication>("release") {
            artifactId = "trivia-sdk-android"
            pom {
                name.set("Trivia SDK Android")
                description.set("Android adapters and a Hilt-free factory for the Trivia SDK.")
                url.set("https://github.com/edblanco/ThinkTap")
            }
            afterEvaluate { from(components["release"]) }
        }
    }
}
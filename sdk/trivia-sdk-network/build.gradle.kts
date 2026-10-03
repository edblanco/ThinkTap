import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.dosparta.core.network"
    compileSdk = 37

    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "BASE_URL", "\"https://opentdb.com/\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://opentdb.com/\"")
        }

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
    implementation(libs.squareup.moshi)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.moshi)
    implementation(libs.javax.inject)
    implementation(libs.squareup.logging.interceptor)

    testImplementation(libs.junit)
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
            artifactId = "trivia-sdk-network"
            pom {
                name.set("Trivia SDK Network")
                description.set("Internal networking support for the Trivia Android SDK.")
                url.set("https://github.com/edblanco/ThinkTap")
            }
            afterEvaluate { from(components["release"]) }
        }
    }
}
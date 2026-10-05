import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    application
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.dosparta.trivia:trivia-sdk-core:0.2.0")
}

application {
    mainClass.set("example.MainKt")
}

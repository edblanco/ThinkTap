plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    group = "com.dosparta.trivia"
    version = "0.2.0"
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        ignoreFailures = false
    }
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
    }
    if (name != "quality-detekt-rules") {
        dependencies.add("detektPlugins", project(":quality-detekt-rules"))
    }
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("detekt")
    }
}

tasks.register("check") {
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("checkJvm") {
    group = "verification"
    description = "Checks the SDK and Android adapters without requiring an Apple toolchain."
    dependsOn(subprojects.map {
        if (it.name == "trivia-sdk-core") "${it.path}:checkJvm" else "${it.path}:check"
    })
}

tasks.register("publishJvm") {
    group = "publishing"
    description = "Publishes common metadata, JVM core, and Android adapters to the SDK repository."
    dependsOn(
        ":trivia-sdk-core:publishKotlinMultiplatformPublicationToLocalRepository",
        ":trivia-sdk-core:publishJvmPublicationToLocalRepository",
        ":trivia-sdk-android:publishReleasePublicationToSdkRepository",
        ":trivia-sdk-network:publishReleasePublicationToSdkRepository"
    )
}

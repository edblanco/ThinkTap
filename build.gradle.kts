// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("jacoco")

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.hilt) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.detekt) apply false
}

// Apply Jacoco only to JVM-based subprojects (avoid instrumenting Android/Robolectric internals)
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    plugins.withType<JavaBasePlugin> {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.8"
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        ignoreFailures = true
    }
}

// Aggregate coverage report for the app's business logic layer only.
// This keeps the summary aligned with the code that is meaningful to test and avoids
// counting generated database, DI, and Compose-only UI classes in the metric.
val coverageProjects = listOf(
    project(":trivia-domain")
)

tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Generates a combined JaCoCo coverage report for the project's business logic layer."

    dependsOn(coverageProjects.flatMap { proj ->
        proj.tasks.withType<Test>()
    })

    reports {
        html.required.set(true)
    }

    sourceDirectories.setFrom(files(coverageProjects.map { proj ->
        proj.projectDir.resolve("src/main/java")
    }))

    classDirectories.setFrom(files(coverageProjects.map { proj ->
        fileTree(mapOf(
            "dir" to proj.layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes").get().asFile,
            "include" to listOf("**/*.class"),
            "exclude" to listOf(
                "**/*Test*.class",
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.class",
                "**/Hilt_*.class",
                "**/Dagger*",
                "**/Generated*"
            )
        ))
    }))

    executionData.setFrom(files(coverageProjects.map { proj ->
        proj.layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec").get().asFile
    }))
}

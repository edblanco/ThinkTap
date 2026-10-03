// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("jacoco")

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.hilt) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.detekt) apply false
}

val jacocoVersion = "0.8.14"

extensions.configure<JacocoPluginExtension> {
    toolVersion = jacocoVersion
}

// Apply Jacoco only to JVM-based subprojects (avoid instrumenting Android/Robolectric internals)
subprojects {
    // NOTE: Detekt 1.23.8 internally calls the deprecated `ReportingExtension.file(String)` when it
    // is applied (DetektPlugin.kt:28). That warning comes from the plugin, not from this build
    // script, so it cannot be fixed here. Remove this note once a Detekt release that drops the
    // deprecated call is available on a stable channel (Detekt 2.x is currently alpha-only and
    // published under the new `dev.detekt` group with breaking rule/API changes).
    apply(plugin = "io.gitlab.arturbosch.detekt")

    plugins.withType<JavaBasePlugin> {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = jacocoVersion
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        ignoreFailures = false
    }

    if (name != "quality-detekt-rules") {
        dependencies {
            add("detektPlugins", project(":quality-detekt-rules"))
        }
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("detekt")
    }
}

// Aggregate coverage report for the app's business logic layer only.
// This keeps the summary aligned with the code that is meaningful to test and avoids
// counting generated database, DI, and Compose-only UI classes in the metric.
val coverageProjects = listOf(
    project(":trivia-domain")
)

val coverageTestTasks = coverageProjects.map { "${it.path}:testDebugUnitTest" }
val coverageExecutionData = files(coverageProjects.map { proj ->
    proj.layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
})
val coverageClassDirectories = files(coverageProjects.map { proj ->
    fileTree(mapOf(
        "dir" to proj.layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
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
})

val validateCoverageInputs = tasks.register("validateCoverageInputs") {
    group = "verification"
    description = "Fails if business-logic coverage data or compiled classes are missing."
    dependsOn(coverageTestTasks)

    doLast {
        coverageExecutionData.forEach { file ->
            check(file.isFile && file.length() > 0) {
                "Missing JaCoCo execution data: $file. Ensure debug unit-test coverage is enabled."
            }
        }
        check(!coverageClassDirectories.isEmpty) {
            "No business-logic classes found for coverage analysis."
        }
    }
}

val jacocoRootReport = tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Generates a combined JaCoCo coverage report for the project's business logic layer."

    dependsOn(validateCoverageInputs)

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    sourceDirectories.setFrom(files(coverageProjects.map { proj ->
        proj.projectDir.resolve("src/main/java")
    }))

    classDirectories.setFrom(coverageClassDirectories)
    executionData.setFrom(coverageExecutionData)
}

tasks.register<JacocoCoverageVerification>("jacocoRootCoverageVerification") {
    group = "verification"
    description = "Requires at least 70% line coverage across the project's business logic layer."
    dependsOn(jacocoRootReport)

    sourceDirectories.setFrom(jacocoRootReport.map { it.sourceDirectories })
    classDirectories.setFrom(coverageClassDirectories)
    executionData.setFrom(coverageExecutionData)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

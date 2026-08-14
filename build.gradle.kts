// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("jacoco")

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.hilt) apply false
    alias(libs.plugins.devtools.ksp) apply false
}

// Apply Jacoco only to JVM-based subprojects (avoid instrumenting Android/Robolectric internals)
subprojects {
    plugins.withType<JavaBasePlugin> {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.8"
        }
    }
}

// Aggregate coverage report across all modules
tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Runs all JVM tests and generates a combined JaCoCo coverage report."

    // Ensure each subproject's JVM test tasks run first
    dependsOn(subprojects.flatMap { proj ->
        proj.tasks.withType<Test>()
    })

    // Enable HTML output
    reports {
        html.required.set(true)
    }

    // Collect source directories from each module
    sourceDirectories.setFrom(files(subprojects.map { proj ->
        proj.projectDir.resolve("src/main/java")
    }))

    // Collect compiled class files, only instrument our packages
    classDirectories.setFrom(files(subprojects.map { proj ->
        fileTree(mapOf(
            "dir" to proj.layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile,
            "include" to listOf("**/com/dosparta/**/*.class"),
            "exclude" to listOf("**/sun/**", "**/org/robolectric/**")
        ))
    }))

    // Collect execution data (.exec, coverage.ec) only from JVM tests
    executionData.setFrom(files(subprojects.map { proj ->
        fileTree(mapOf(
            "dir" to proj.layout.buildDirectory.dir("jacoco").get().asFile,
            "include" to listOf("*.exec", "*.ec")
        ))
    }))
}

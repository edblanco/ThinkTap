import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    jacoco
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    sourceSets {
        main { kotlin.srcDir("src/main/java") }
        test { kotlin.srcDir("src/test/java") }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    useJUnit()
}

val validateCoverageInputs = tasks.register("validateCoverageInputs") {
    group = "verification"
    dependsOn(tasks.test)
    doLast {
        val executionData = layout.buildDirectory.file("jacoco/test.exec").get().asFile
        check(executionData.isFile && executionData.length() > 0L) {
            "Missing JaCoCo execution data: $executionData"
        }
        check(tasks.jacocoTestReport.get().classDirectories.asFileTree.files.any { it.extension == "class" }) {
            "No SDK core classes found for coverage analysis."
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(validateCoverageInputs)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "trivia-sdk-core"
            pom {
                name.set("ThinkTap Trivia SDK Core")
                description.set("UI-independent trivia rules and session orchestration.")
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri(providers.gradleProperty("sdkRepository").getOrElse("${rootProject.layout.buildDirectory.get()}/repository"))
        }
    }
}

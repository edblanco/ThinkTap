import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    jacoco
}

kotlin {
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    val framework = XCFramework("TriviaCore")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "TriviaCore"
            isStatic = true
            export(libs.kotlinx.coroutines.core)
            framework.add(this)
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
                implementation(libs.mockk)
            }
        }
    }
}

jacoco {
    toolVersion = "0.8.14"
}

val jvmTests = tasks.named<Test>("jvmTest") {
    useJUnit()
}
val coreClasses = files(
    layout.buildDirectory.dir("classes/kotlin/jvm/main")
)
val coverageData = layout.buildDirectory.file("jacoco/jvmTest.exec")

val validateCoverageInputs = tasks.register("validateCoverageInputs") {
    group = "verification"
    dependsOn(jvmTests)
    doLast {
        val executionData = coverageData.get().asFile
        check(executionData.isFile && executionData.length() > 0L) {
            "Missing JaCoCo execution data: $executionData"
        }
        check(coreClasses.asFileTree.files.any { it.extension == "class" }) {
            "No SDK core classes found for coverage analysis."
        }
    }
}

val coverageReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(validateCoverageInputs)
    executionData(coverageData)
    classDirectories.setFrom(coreClasses)
    sourceDirectories.setFrom("src/commonMain/kotlin", "src/jvmMain/kotlin")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val coverageVerification = tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(coverageReport)
    executionData(coverageData)
    classDirectories.setFrom(coreClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(coverageVerification)
}

tasks.register("checkJvm") {
    group = "verification"
    dependsOn(coverageVerification, "detekt")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    setSource(files(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/iosMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmTest/kotlin"
    ))
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("ThinkTap Trivia SDK Core")
            description.set("Multiplatform trivia rules and session orchestration for JVM and iOS.")
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri(providers.gradleProperty("sdkRepository").getOrElse("${rootProject.layout.buildDirectory.get()}/repository"))
        }
    }
}

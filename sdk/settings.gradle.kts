pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ThinkTap Trivia SDK"
include(":trivia-sdk-core", ":trivia-sdk-android", ":trivia-sdk-network", ":quality-detekt-rules")

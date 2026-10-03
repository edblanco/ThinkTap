pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri(providers.gradleProperty("sdkRepository").getOrElse("sdk/build/repository"))
            content { includeGroup("com.dosparta.trivia") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Trivia Game 2"
include(":app")
include(":core-ui")
include(":feature-trivia-ui")
include(":quality-detekt-rules")

if (providers.gradleProperty("useLocalTriviaSdk").getOrElse("false").toBoolean()) {
    includeBuild("sdk")
}

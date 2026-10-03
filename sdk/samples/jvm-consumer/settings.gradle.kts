pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri(providers.gradleProperty("sdkRepository").getOrElse("../../build/repository"))
            content { includeGroup("com.dosparta.trivia") }
        }
        mavenCentral()
    }
}

rootProject.name = "standalone-trivia-consumer"

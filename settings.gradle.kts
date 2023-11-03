pluginManagement {
    repositories {
        maven("https://maven.google.com/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.google.com/")
        mavenCentral()
    }
}

rootProject.name = "SMS Gateway Press Client"
include(":app")

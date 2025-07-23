pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://dl.bitdrift.io/sdk/android-maven")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = "https://dl.bitdrift.io/sdk/android-maven") {
            content {
                includeGroup("io.bitdrift")
            }
        }
    }
}

include(":app")

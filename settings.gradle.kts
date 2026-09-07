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

rootProject.name = "f21-campo"

include(
    ":app",
    ":domain",
    ":data",
    ":files",
    ":rinex",
    ":processing",
    ":f21",
    ":hnproject",
    ":receiver-api",
    ":receiver-manual",
)

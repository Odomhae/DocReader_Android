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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DocReader"
include(":app")
include(":core:model")
include(":core:data")
include(":core:files")
include(":core:ui")
include(":core:ads")
include(":feature:home")
include(":feature:settings")
include(":viewer:pdf")
include(":viewer:web")
include(":viewer:hwp")
include(":viewer:docx")
include(":viewer:txt")

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
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        
        // ✅ Sora Editor Repository ထည့်ပါ
        maven { url = uri("https://maven.pkg.github.com/Rosemoe/sora-editor") }
    }
}

rootProject.name = "AidePro"
include(":app")
include(":core:buildsystem")
include(":core:editor")
include(":core:ai")
include(":core:utils")
include(":core:terminal")

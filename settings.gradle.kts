pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://files.minecraftforge.net/maven")
        }
        maven {
            name = "FancyGradle"
            url = uri("https://gitlab.com/api/v4/projects/26758973/packages/maven")
        }
        maven {
            name = "Garden of Fancy"
            url = uri("https://maven.gofancy.wtf/releases")
        }
    }
}

val baseProjectName = providers.gradleProperty("baseProjectName").get()

// Patcher project
rootProject.name = "$baseProjectName-Patcher"

include(":$baseProjectName-Base")
include(":$baseProjectName-Patched")

project(":$baseProjectName-Base").projectDir = file("src/${baseProjectName.toLowerCase()}/base")
project(":$baseProjectName-Patched").projectDir = file("src/${baseProjectName.toLowerCase()}/patched")

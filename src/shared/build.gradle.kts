import codechicken.diffpatch.util.PatchMode
import net.minecraftforge.gradle.patcher.tasks.GenerateBinPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.mcp.tasks.GenerateSRG
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace

val mappingsChannel:    String by project
val mappingsVersion:    String by project
val versionForge:       String by project

plugins {
    java
    id("net.minecraftforge.gradle")
    id("wtf.gofancy.fancygradle")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

fancyGradle {
    patches {
        asm
        codeChickenLib
        coremods
        resources
        mergetool
    }
}

repositories {
    mavenCentral()
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
}

sourceSets {
    main {
        java {
            srcDir("src/java")
        }
        resources {
            srcDir("src/resources")
        }
    }
}
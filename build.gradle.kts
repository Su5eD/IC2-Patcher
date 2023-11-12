import fr.brouillard.oss.jgitver.GitVersionCalculator
import fr.brouillard.oss.jgitver.Strategies
import net.minecraftforge.gradle.common.util.RunConfig
import wtf.gofancy.fancygradle.script.extensions.deobf
import net.minecraftforge.gradle.patcher.task.GenerateBinPatches
import wtf.gofancy.fancygradle.patch.Patch

buildscript {
    dependencies { 
        classpath(group = "fr.brouillard.oss", name = "jgitver", version = "0.14.0")
    }
}

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.minecraftforge.gradle") version "5.0.11"
    id("de.undercouch.download") version "4.1.1"
    id("wtf.gofancy.fancygradle") version "1.1.+"
}

evaluationDependsOnChildren()
java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val versionMc = "1.12.2"
val mappingsChannel: String by project
val mappingsVersion: String by project

val versionIC2: String by project
val versionJEI: String by project
val versionForge: String by project

version = getGitVersion()
group = "mods.su5ed"
setProperty("archivesBaseName", "ic2patcher")

minecraft {
    mappings(mappingsChannel, mappingsVersion)

    runs {
        val config = Action<RunConfig> {
            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP,COREMODLOG",
                    "forge.logging.console.level" to "debug"
                )
            )
            workingDirectory = project.file("run").canonicalPath
            source(sourceSets["main"])
        }

        create("client", config)
        create("server", config)
    }
}

fancyGradle {
    patches {
        Patch.RESOURCES
        Patch.COREMODS
        Patch.CODE_CHICKEN_LIB
        Patch.ASM
    }
}

repositories {
    maven {
        // JEI Repository
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        // Mirror Maven for JEI
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
    maven {
        // IC2 Repository
        name = "ic2"
        url = uri("https://maven.ic2.player.to/")
    }
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
    
    implementation(project(":IC2-Patched"))
//    implementation(fg.deobf(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI))
    compileOnly(fg.deobf(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI))
}

tasks {
    named<ProcessResources>("processResources") { 
        // this will ensure that this task is redone when the versions change.
        inputs.property("version", project.version)
        inputs.property("mcversion", versionMc)
    
        filesMatching("mcmod.info") {
            // replace version and mcversion
            expand(
                    "version" to project.version,
                    "mcversion" to versionMc
            )
        }
    }
    
    named("jar") {
        enabled = false
    }
            
    register<Jar>("devJar") {
        val generateDevBinPatches = project(":IC2-Patched").tasks.getByName<GenerateBinPatches>("generateDevBinPatches")
        dependsOn(generateDevBinPatches)
        from(sourceSets.main.get().output) {
            exclude("patches");
        }
        from(generateDevBinPatches.output)
        
        manifest { 
            attributes(
                    "FMLCorePlugin" to "mods.su5ed.ic2patcher.asm.PatcherFMLPlugin",
                    "FMLCorePluginContainsFMLMod" to true
            )
        }
    }
    
    register<Jar>("releaseJar") {
        archiveClassifier.set("")
        val generateBinPatches = project(":IC2-Patched").tasks.getByName<GenerateBinPatches>("generateBinPatches")
        dependsOn(generateBinPatches)
        
        from(sourceSets.main.get().output)
        manifest { 
            attributes(
                    "FMLCorePlugin" to "mods.su5ed.ic2patcher.asm.PatcherFMLPlugin",
                    "FMLCorePluginContainsFMLMod" to true
            )
        }
    }
    
    register("setup") {
        group = "env setup"
        dependsOn(":IC2-Patched:setup")
    }
    
    whenTaskAdded { 
        if (name.startsWith("prepareRun")) {
            dependsOn("devJar")
        }
    }
}

reobf {
    create("jar") {
        dependsOn("releaseJar")
    }
}

artifacts { 
    archives(tasks.getByName("releaseJar"))
}

sourceSets {
    main {
        resources {
            srcDir("src/main/generatedResources")
        }
    }
}

fun getGitVersion(): String {
    val jgitver = GitVersionCalculator.location(rootDir)
            .setNonQualifierBranches("master")
            .setVersionPattern("\${M}\${<m}\${<meta.COMMIT_DISTANCE}\${-~meta.QUALIFIED_BRANCH_NAME}")
            .setStrategy(Strategies.PATTERN)
    return jgitver.version
}

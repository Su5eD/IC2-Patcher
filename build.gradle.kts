import fr.brouillard.oss.jgitver.GitVersionCalculator
import fr.brouillard.oss.jgitver.Strategies
import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.gradle.patcher.tasks.GenerateBinPatches
import wtf.gofancy.fancygradle.script.extensions.deobf

buildscript {
    dependencies { 
        classpath(group = "fr.brouillard.oss", name = "jgitver", version = "0.14.0")
    }
}

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.minecraftforge.gradle") version "5.1.+"
    id("de.undercouch.download") version "4.1.1"
    id("wtf.gofancy.fancygradle") version "1.1.+"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val versionMc = "1.12.2"
val mappingsChannel: String by project
val mappingsVersion: String by project

val versionForge: String by project
val versionJEI: String by project
val extraUtils2Source: String by project

val taskGroup: String by project
val baseArchiveName: String by project
val baseProjectName: String by project

version = getGitVersion()
setProperty("archivesBaseName", baseArchiveName)
evaluationDependsOnChildren()

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
        asm
        codeChickenLib
        coremods
        resources
        mergetool
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
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")

    implementation(project(":$baseProjectName-Patched"))
    implementation(fg.deobf(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI))
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

    //TODO: Add Generate Binary Patches ~ Dev 1.12 task and configure this one to use patches from that.
//    register<Jar>("Dev Jar ~ Patcher") {
//        group = taskGroup;
//        archiveFileName.set("${baseArchiveName}-dev.jar")
//
//        from(sourceSets.main.get().output) {
//            exclude("patches");
//        }
//
//        manifest {
//            attributes(
//                "FMLCorePlugin" to "${group}.${baseProjectName.toLowerCase()}patcher.asm.PatcherFMLPlugin",
//                "FMLCorePluginContainsFMLMod" to true
//            )
//        }
//    }

    register<Jar>("Release Jar ~ Patcher") {
        group = taskGroup;
        val binPatches = project(":$baseProjectName-Patched").tasks.getByName<GenerateBinPatches>("Generate Binary Patches ~ Patched");
        dependsOn(binPatches)
        mustRunAfter(binPatches)

        archiveClassifier.set("")
        from(sourceSets.main.get().output)
        manifest {
            attributes(
                "FMLCorePlugin" to "$group.${baseProjectName.toLowerCase()}patcher.asm.PatcherFMLPlugin",
                "FMLCorePluginContainsFMLMod" to true
            )
        }
    }

    register("Setup $baseProjectName Source") {
        group = taskGroup
        dependsOn(project(":$baseProjectName-Patched").tasks.getByName<Copy>("Setup Source ~ Patched"))
    }

//    whenTaskAdded {
//        if (name.startsWith("prepareRun")) {
//            dependsOn(project(":UX2-Patched").tasks.getByName("patchRunJar"))
//            dependsOn("devJar")
//            dependsOn("patchModifyClassPath")
//            dependsOn("patchGenerateObfToSrg")
//            dependsOn("patchExtractMappingsZip")
//        }
//    }
}

reobf {
    create("jar") {
        dependsOn("Release Jar ~ Patcher")
    }
}

artifacts {
    archives(tasks.getByName("Release Jar ~ Patcher"))
}

sourceSets {
    main {
        resources {
            srcDir("src/main/generated")
        }
    }
}

/**
 * @author Su5eD
 */
fun getGitVersion(): String {
    val jgitver = GitVersionCalculator.location(rootDir)
            .setNonQualifierBranches("master")
            .setVersionPattern("\${M}\${<m}\${<meta.COMMIT_DISTANCE}\${-~meta.QUALIFIED_BRANCH_NAME}")
            .setStrategy(Strategies.PATTERN)
    return jgitver.version
}
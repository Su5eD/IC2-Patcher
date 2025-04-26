import codechicken.diffpatch.util.PatchMode
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.patcher.tasks.GenerateBinPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.mcp.tasks.GenerateSRG

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("wtf.gofancy.fancygradle")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val baseProjectName:    String by project
val mappingsChannel:    String by project
val mappingsVersion:    String by project
val versionIC2:         String by project
val versionJEI:         String by project
val versionBuildCraft:  String by project
val versionForge:       String by project

evaluationDependsOn(":$baseProjectName-Base")

val shade:          Configuration by configurations.creating
val patchedMod:     Configuration by configurations.creating
val projectBase:    Project = project(":$baseProjectName-Base")
val taskGroup:      String = "$baseProjectName patcher ~ patched"
val patchedJar:     File = File(buildDir, "applyPatches/patched.jar")
val baseSrc:        File = File(projectBase.projectDir, "src")
val src:            File = File(projectDir, "src")
val libs:           File = File(buildDir, "libs")

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
    maven {
        name = "ic2"
        url = uri("https://maven2.ic2.player.to/")
    }
    maven {
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        name = "BuildCraft"
        url = uri("https://mod-buildcraft.com/maven")
    }
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")

    patchedMod(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112")
    implementation(project(":api"))
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-lib", version = versionBuildCraft)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-main", version = versionBuildCraft)

    val ejml = create(group = "com.googlecode.efficient-java-matrix-library", name = "core", version = "0.26")
    implementation(ejml)
    shade(ejml)
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

reobf {
    create("jar") {
        dependsOn("shadowJar")
    }
}

tasks {
    register<ApplyPatches>("Apply Patches ~ Patched") {
        setup(this)
        val baseSourceJar = projectBase.tasks.getByName<Jar>("Source Jar ~ Base")
        dependsOn(baseSourceJar)

        base.set(baseSourceJar.archiveFile.get().asFile)
        patches.set(getPatchesDirectory())
        rejects.set(File(buildDir, "applyPatches/rejects.zip"))
        output.set(patchedJar)
        patchMode.set(PatchMode.OFFSET)
        isPrintSummary = true
    }

    register<Copy>("Setup Source ~ Patched") {
        setup(this)
        filteringCharset = "UTF-8"
        includeEmptyDirs = false

        dependsOn("Apply Patches ~ Patched")

        from(zipTree(patchedJar)) {
            include {
                it.path.startsWith("ic2") &&
                !it.path.startsWith("ic2/sounds") &&
                !it.path.startsWith("ic2/profiles") &&
                !it.path.startsWith("org")
            }

            eachFile { this.path = "java/" + this.path }
        }

        from(fileTree(baseSrc)) {
            exclude {
                it.path.startsWith("java") ||
                it.name == "version.txt"
            }
        }

        into(src)

        doFirst {
            // Ask the developer if they are sure they want to rollback changes to the source.
            if (correctSrcExists() && !project.hasProperty("skipSetupConfirmation")) {
                println("Unsaved changes detected! Are you sure you want to run the setup again? [y/n]")
                val response = readLine()?.toLowerCase()?.trim() ?: "no"
                if (response != "yes" && response != "y") throw GradleException("Task execution aborted by the user. Run \"Generate Patches ~ Base\" to save the changes!")
            }

            if (!correctSrcExists() && src.exists()) {
                // Cleanup the files, as files that are missing in the different IC2 version would be left otherwise.
                src.deleteRecursively()
            }
        }

        doLast {
            // Adds a cached IC2 version, to know when a source is not up-to-date.
            val ver = File(src, "version.txt")
            if (ver.exists()) {
                println("Overriding old version.txt!")
                ver.delete()
            }
            ver.writeText(versionIC2)
        }
    }

    register<Jar>("Source Jar ~ Patched") {
        setup(this)
        if (!correctSrcExists()) dependsOn("Setup Source ~ Patched")

        archiveClassifier.set("Sources")

        from(sourceSets.main.get().allSource)
    }

    register("Build ~ Patched") {
        setup(this)
        dependsOn("build")
    }

    named<ShadowJar>("shadowJar") {
        dependsOn("classes")
        configurations = listOf(project.configurations["shade"])
        archiveClassifier.set("")
    }

    register<GeneratePatches>("Generate Patches ~ Patched") {
        setup(this)

        val baseSourceJar = projectBase.tasks.getByName<Jar>("Source Jar ~ Base")
        val sourceJar = getByName<Jar>("Source Jar ~ Patched")

        dependsOn(baseSourceJar)
        dependsOn(sourceJar)

        base.set(baseSourceJar.archiveFile)
        modified.set(sourceJar.archiveFile)
        output.set(getPatchesDirectory())
        isPrintSummary = true
    }

    register<GenerateBinPatches>("Generate Binary Patches ~ Patched") {
        setup(this)
        val createMcpToSrg = getByName<GenerateSRG>("createMcpToSrg")
        val patchedJar = getByName("Build ~ Patched")
        dependsOn(createMcpToSrg)
        dependsOn(patchedJar)
        patchSets.setFrom(getPatchesDirectory())
        cleanJar.set(patchedMod.singleFile)
        dirtyJar.set(File(libs, "$baseProjectName-Patched.jar"))
        output.set(File(rootDir, "src/main/generated/patches/${getPatchesDirectory().name}/patches.pack.lzma"))
        srg.set(createMcpToSrg.output)
        args.set(listOf(
            "--output", "{output}",
            "--patches", "{patches}",
            "--srg", "{srg}",
            "--clean", "{clean}",
            "--create", "{dirty}",
            "--prefix", "binpatch/merged"
        ))
    }
}

fun setup(task: Task) {
    task.group = taskGroup
    task.outputs.upToDateWhen { correctSrcExists() }
}

fun correctBaseSrcExists(): Boolean {
    return File(baseSrc, "version.txt").exists() && File(baseSrc, "version.txt").readText() == versionIC2
}

fun correctSrcExists(): Boolean {
    return File(src, "version.txt").exists() && File(src, "version.txt").readText() == versionIC2
}

fun getPatchesDirectory(): File {
    val patchDir = File(projectDir, "patches")

    patchDir.listFiles { _, name ->
        name.startsWith("[") && name.endsWith("]")
    }?.forEach { file ->
        val name = file.name.toString()
        val versions = name.replace("[", "").replace("]", "").split(",")
        if (
            (versions.size == 1 && versionIC2 == versions[0]) ||
            (versions.size == 2 && compareVersions(versionIC2, versions[0]) && (versions[1] == "+" || compareVersions(versions[1], versionIC2)))
        ) {
//            println("Found patches for version $versionIC2! File: $file")
            return file
        }
    }

    throw GradleException("Unable to determine patch directory! Setup can not continue.")
}

/**
 * Compares two versions separated by dots. Doesn't work with versions schema containing letters.
 * Truth table:
 *  v1 > v2 => true
 *  v1 == v2 => true
 *  v1 < v2 => false
 *  v1 // v2 contain chars -> Integer parsing Exception
 */
fun compareVersions(v1:String, v2:String): Boolean {
    val v1s = v1.split(".");
    val v2s = v2.split(".");

    val length: Int = if (v1s.size > v2s.size) { v2s.size } else { v1s.size }
    for (i in 0 until length) {
        val v1i = Integer.parseInt(v1s[i]);
        val v2i = Integer.parseInt(v2s[i])
        if (v1i > v2i) {
            return true
        } else if (v2i > v1i) {
            return false;
        }
    }
    return v1s.size >= v2s.size
}
import codechicken.diffpatch.util.PatchMode
import net.minecraftforge.gradle.common.tasks.JarExec
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val baseProjectName:    String by project
val mappingsChannel:    String by project
val mappingsVersion:    String by project
val taskGroup:          String = "$baseProjectName patcher ~ base"
val versionIC2:         String by project
val versionJEI:         String by project
val versionBuildCraft:  String by project
val versionForge:       String by project
val versionVineflower: String by project

val patchedMod:         Configuration by configurations.creating
val shade:              Configuration by configurations.creating
val decompiledJar:      File = File(buildDir, "decompileMod/source.jar")
val patchedJar:         File = File(buildDir, "applyPatches/patched.jar")
val src:                File = File(projectDir, "src")

tasks {
    register<JarExec>("Decompile $baseProjectName") {
        setup(this)

        tool.set("org.vineflower:vineflower:$versionVineflower")
        setRuntimeJavaVersion(17)
        val input = patchedMod.singleFile.absolutePath
        outputs.file(decompiledJar)
        args.set(listOf(
            "--ascii-strings=1", // Encode non-ASCII characters in string and character literals as Unicode escapes.
            "--log-level=debug", // Log Level
            input,
            decompiledJar.absolutePath
        ))
    }

    register<ApplyPatches>("Apply Patches ~ Base") {
        setup(this)
        dependsOn("Decompile $baseProjectName")

        base.set(decompiledJar)
        patches.set(getPatchesDirectory())
        rejects.set(File(buildDir, "applyPatches/rejects.zip"))
        output.set(patchedJar)
        patchMode.set(PatchMode.OFFSET)

        isPrintSummary = true
    }

    register<Copy>("Setup Source ~ Base") {
        setup(this)
        includeEmptyDirs = false

        dependsOn("Apply Patches ~ Base")

        from(zipTree(patchedJar)) {
            include {
                it.path.startsWith("ic2") &&
                !it.path.startsWith("ic2/sounds") &&
                !it.path.startsWith("ic2/profiles") &&
                !it.path.startsWith("org")
            }

            eachFile { this.path = "java/" + this.path }
        }

        from(zipTree(patchedMod.singleFile.absolutePath)) {
            exclude {
                (
                    it.path.startsWith("ic2") &&
                    !it.path.startsWith("ic2/sounds") &&
                    !it.path.startsWith("ic2/profiles")
                ) ||
                it.path.startsWith("org") ||
                // We don't copy META-INF as that contains signature data, which ofc will be incorrect and cause security exceptions.
                it.path.startsWith("META-INF")
            }

            eachFile { this.path = "resources/" + this.path }
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

    register("Build ~ Base") {
        setup(this)
        dependsOn("build")
    }

    register<Jar>("Source Jar ~ Base") {
        setup(this)
        if (!correctSrcExists()) dependsOn("Setup Source ~ Base")

        archiveClassifier.set("Sources")

        from(sourceSets.main.get().allSource)
    }

    register<GeneratePatches>("Generate Patches ~ Base") {
        setup(this)
        val sourceJar = getByName<Jar>("Source Jar ~ Base")
        dependsOn(sourceJar)
        val patchDir = getPatchesDirectory()

        base.set(decompiledJar)
        modified.set(sourceJar.outputs.files.singleFile)
        output.set(patchDir)
        isPrintSummary = true

        doLast {
            // Those patches are causing issues and are generated because Source Jar doesn't contain those, so we just remvoe them.
            File(patchDir, "org").deleteRecursively()
            File(patchDir, "META-INF").deleteRecursively()
        }
    }
}

artifacts {
    archives(tasks.getByName("Source Jar ~ Base"))
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

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")

    patchedMod(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112", classifier = "dev")
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-lib", version = versionBuildCraft)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-main", version = versionBuildCraft)

    val ejml = create(group = "com.googlecode.efficient-java-matrix-library", name = "core", version = "0.26")
    implementation(ejml)
    shade(ejml)
}

fun setup(task: Task) {
    task.group = taskGroup
    task.outputs.upToDateWhen { correctSrcExists() }
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
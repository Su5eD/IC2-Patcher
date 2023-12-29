import codechicken.diffpatch.util.PatchMode
import org.apache.commons.io.FileUtils
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.patcher.tasks.GenerateBinPatches
import net.minecraftforge.gradle.patcher.tasks.GeneratePatches
import net.minecraftforge.gradle.patcher.tasks.ApplyPatches
import net.minecraftforge.gradle.mcp.tasks.GenerateSRG
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Pattern

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("wtf.gofancy.fancygradle")
}

evaluationDependsOn(":IC2-Base")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val mappingsChannel: String by project
val mappingsVersion: String by project
val taskGroup: String = "IC2 Patcher"
val versionIC2: String by project
val versionJEI: String by project
val versionBuildCraft: String by project
val versionForge: String by project

val patchedJar: File = File(buildDir, "applyPatches/output.jar")
val patchesDir: File = file(getPatchesDirectory() + "/minecraft")

val ic2Dev: Configuration by configurations.creating
val ic2Clean: Configuration by configurations.creating
val shade: Configuration by configurations.creating

group = "net.industrial-craft"
version = "$versionIC2-SNAPSHOT"

minecraft {
    mappings(mappingsChannel, mappingsVersion)
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

tasks {
    register<ApplyPatches>("applyPatches") {
        val baseSourceJar = project(":IC2-Base").tasks.getByName<Jar>("sourceJarWithResources")
        dependsOn(baseSourceJar)

        group = taskGroup
        base.set(baseSourceJar.archiveFile.get().asFile)
        patches.set(patchesDir)
        rejects.set(File(buildDir, "$name/rejects.zip"))
        output.set(patchedJar)
        patchMode.set(PatchMode.OFFSET)

        isPrintSummary = true
    }
    
    register<Copy>("extractSources") {
        group = taskGroup
        dependsOn("applyPatches")
        
        from(zipTree(patchedJar)) {
            include("ic2/**")
        }
        into("src/main/java")
    }
    
    register<Copy>("extractResources") {
        group = taskGroup
        dependsOn("extractSources")
            
        from(zipTree(patchedJar)) {
            exclude("ic2/**", "org/**")
        }
        into("src/main/resources")
    }
    
    register<Jar>("sourceJar") {
        group = taskGroup
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allJava)
    }

    register<Jar>("sourceJarWithResources") {
        group = taskGroup
        archiveClassifier.set("sources-with-resources")
        from(sourceSets.main.get().allSource)
    }
    
    register<GeneratePatches>("generatePatches") {
        group = taskGroup
        val baseSourceJar = project(":IC2-Base").tasks.getByName<Jar>("sourceJar")
        val sourceJar = getByName<Jar>("sourceJar")
        val outputDir = patchesDir
        dependsOn(sourceJar)
        dependsOn(baseSourceJar)

        base.set(baseSourceJar.archiveFile.get().asFile)
        modified.set(sourceJar.archiveFile.get().asFile)
        output.set(outputDir)
        isPrintSummary = true

        doLast {
            val outputPath = outputDir.toPath()
            Files.walk(outputPath)
                    .filter { path ->
                        val relative = outputPath.relativize(path).toString()
                        relative.isNotEmpty() && (!relative.startsWith("ic2") || relative.startsWith("ic2\\profiles") || relative.startsWith("ic2\\sounds")) && path.toFile().isDirectory
                    }
                    .map(Path::toFile)
                    .forEach(FileUtils::deleteDirectory)
        }
    }
    
    register("setup") {
        group = taskGroup
        dependsOn("extractResources")
    }
    
    named<ShadowJar>("shadowJar") {
        dependsOn("classes", "extractSources")
    
        configurations = listOf(project.configurations["shade"])
        archiveClassifier.set("")
        from("src/main/java/ic2") {
            include("profiles/**")
            include("sounds/**")
            into("ic2")
        }
    }
    
    register<GenerateBinPatches>("generateBinPatches") {
        dependsOn("reobfJar")
        group = taskGroup
        cleanJar.set(ic2Clean.singleFile)
        dirtyJar.set(File(buildDir, "reobfJar/output.jar"))
        output.set(File(rootDir, "src/main/generatedResources/patches/" + getPatchesDirectory() + "/ic2patches.pack.lzma"))
        configureBinPatchTask(this)
    }

    register<GenerateBinPatches>("generateDevBinPatches") {
        val shadowJar = getByName<ShadowJar>("shadowJar")
        dependsOn(shadowJar)
        group = taskGroup
        cleanJar.set(ic2Dev.singleFile)
        dirtyJar.set(shadowJar.archiveFile.get().asFile)
        output.set(File(buildDir, "$name/patches/" + getPatchesDirectory() + "/ic2patches.pack.lzma"))
        configureBinPatchTask(this)
    }

    /**
     * Used to replace classes in the jar used for running the game in the IDE.
     * Some classes (like networking) aren't functional after recompiling them, so those are getting replaced.
     */
    register("patchRunJar") {
        val shadowJar = getByName<ShadowJar>("shadowJar")
        dependsOn(shadowJar)
        mustRunAfter(shadowJar)

        val projectJarFile = shadowJar.archiveFile.get().asFile
        val patchedJar = File(projectJarFile.toString().replace(".jar", "-patched.jar"))

        //TODO: Add separation between versions, so you can specify version range for replacement classes. For now those are required classes for 164 to boot.
        val classesToReplace = listOf("assets/.*", "ic2/sounds/.*")
        val tries = 3;

        doLast { for (i in 1 until tries+1) { try {
            val patchedOut = JarOutputStream(patchedJar.outputStream())
            val projectJar = JarFile(projectJarFile)
            val sourceJar = JarFile(ic2Dev.singleFile)
            val stats = HashMap<String, Int>()
            classesToReplace.forEach { regex -> stats[regex] = 0 }

            projectJar.use { input -> sourceJar.use { inputOg ->
                val entries = input.entries()
                val sourceEntries = ArrayList<String>()
                for (entry in inputOg.entries()) sourceEntries.add(entry.name)
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement()
                    var replaced = false

                    for (regex in classesToReplace) {
                        if (Pattern.matches(regex, entry.name) && !entry.name.endsWith("/")) {
                            if (sourceEntries.contains(entry.name)) {
                                entry = sourceJar.getJarEntry(entry.name);
                                patchedOut.putNextEntry(entry)
                                patchedOut.write(sourceJar.getInputStream(entry).readBytes())
                                replaced = true
                                stats[regex] = (stats[regex] ?: 0) + 1
                            } else {
                                println("Couldn't find entry " + entry.name + " in original jar!")
                            }
                            break
                        }
                    }
                    if (!replaced) {
                        patchedOut.putNextEntry(entry)
                        patchedOut.write(projectJar.getInputStream(entry).readBytes())
                    }
                    patchedOut.closeEntry()
                }
                patchedOut.close()
                println("Attempt $i was successful! Out of " + input.size() + " entries replaced:")
                stats.forEach { (key, value) -> println("> $value entries under regex $key") }
            } }
            projectJar.close()
            Files.delete(projectJarFile.toPath())
            Files.move(patchedJar.toPath(), projectJarFile.toPath())
            break
        } catch (e: Exception) {
            if (i >= tries) throw e
            println("Exception caught while trying to patch run jar. Attempt: $i | Message: " + e.localizedMessage)
        } } }
    }

    named("compileJava") {
        dependsOn("setup")
    }
    
    named("processResources") {
        dependsOn("extractResources")
    }
}

fun configureBinPatchTask(task: GenerateBinPatches) {
    val createMcpToSrg = tasks.getByName<GenerateSRG>("createMcpToSrg")
    task.dependsOn(createMcpToSrg)
//    task.addPatchSet(patchesDir)
    task.patchSets.setFrom(patchesDir)

    task.args.set(listOf(
            "--output", "{output}",
            "--patches", "{patches}",
            "--srg", "{srg}",
//            "--legacy",
            "--clean", "{clean}",
            "--create", "{dirty}",
            "--prefix", "binpatch/merged"
    ))

    task.srg.set(createMcpToSrg.output)
}

reobf {
    create("jar") {
        dependsOn("shadowJar")
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

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-${versionForge}")
    
    ic2Dev(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112", classifier = "dev")
    ic2Clean(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112")
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-lib", version = versionBuildCraft)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-main", version = versionBuildCraft)
    
    val ejml = create(group = "com.googlecode.efficient-java-matrix-library", name = "core", version = "0.26")
    implementation(ejml)
    shade(ejml)
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

/**
 * Used to get patches directory based on the IC2 Version specified in the Gradle properties.
 */
fun getPatchesDirectory(): String {
    project(":IC2-Patched").projectDir.listFiles { _, name ->
        name.startsWith("patches[") && name.endsWith("]")
    }?.forEach { file ->
        val name = file.name.toString()
        val versions = name.substring(name.indexOf("[")+1, name.indexOf("]")).split(",")
        if (versions.size == 2) {
            if (compareVersions(versionIC2, versions[0])) {
                if (versions[1] == "+" || !compareVersions(versionIC2, versions[1])) {
                    return "patches[${versions[0]},${versions[1]}]"
                }
            }
        }
    }
    return "patches"
}

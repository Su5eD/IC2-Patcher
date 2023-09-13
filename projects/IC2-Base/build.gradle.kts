import codechicken.diffpatch.util.PatchMode
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.common.task.JarExec
import net.minecraftforge.gradle.patcher.task.TaskApplyPatches
import net.minecraftforge.gradle.patcher.task.TaskGeneratePatches
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile

plugins {
    java
    id("de.undercouch.download")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

val mappingsChannel: String by project
val mappingsVersion: String by project
val taskGroup: String = "IC2 Patcher"
val versionIC2: String by project
val versionJEI: String by project
val versionBuildCraft: String by project
val versionForgeFlower: String by project
val versionForge: String by project

val decompiledJar: File = File(buildDir, "decompileIC2/output.jar")
val patchedJar: File = File(buildDir, "applyPatches/output.jar")
val processedJar: File = File(buildDir, "applyStyle/output.jar")
val processedJarPatched: File = File(buildDir, "applyStylePatched/output.jar")

val ic2: Configuration by configurations.creating
val shade: Configuration by configurations.creating

group = "net.industrial-craft"
version = "$versionIC2-SNAPSHOT"

minecraft {
    mappings(mappingsChannel, mappingsVersion)
}

tasks {
    register<JarExec>("decompileIC2") {
        group = taskGroup
        tool = "net.minecraftforge:forgeflower:$versionForgeFlower"
        val input = ic2.singleFile.absolutePath
        outputs.file(decompiledJar)
        args = arrayOf("-din=1", "-rbr=1", "-dgs=1", "-asc=1", "-rsy=1", "-iec=1", "-jvn=1", "-log=TRACE", input, decompiledJar.absolutePath)
    }
    
    register<ApplyAstyle>("applyStyle") {
        group = taskGroup
        dependsOn("decompileIC2")
        input.set(decompiledJar)
        output.set(processedJar)
    }
    
    register<TaskApplyPatches>("applyPatches") {
        group = taskGroup
        dependsOn("applyStyle")
        base = processedJar

        patches = getPatchesDirectory()
        rejects = File(buildDir, "$name/rejects.zip")
        output = patchedJar
        patchMode = PatchMode.OFFSET
        
        isPrintSummary = true
    }
    
    register<ApplyAstyle>("applyStylePatched") {
        group = taskGroup
        dependsOn("applyPatches")
        input.set(patchedJar)
        output.set(processedJarPatched)
    }
    
    register<Copy>("extractSources") {
        group = taskGroup
        dependsOn("applyStylePatched")
        
        from(zipTree(processedJarPatched)) {
            exclude("ic2/api/energy/usage.txt")
        }
        into("src/main/java")
    }
    
    register<Copy>("extractResources") {
        group = taskGroup
        dependsOn("extractSources")
            
        from(zipTree(decompiledJar)) {
            exclude("ic2/**", "org/**")
        }
        into("src/main/resources")
    }
    
    register<Jar>("sourceJar") {
        dependsOn("classes")
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allJava) {
            exclude("*.txt")
        }
    }

    register<Jar>("sourceJarW-ODep") {
        // This dependency didn't allow for generatePatches to work correctly.
//        dependsOn("classes")
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allJava) {
            exclude("*.txt")
        }
    }

    register<Jar>("sourceJarWithResources") {
        dependsOn("classes","extractResources")
        archiveClassifier.set("sources-with-resources")
        from(sourceSets.main.get().allSource)
    }
    
    register<TaskGeneratePatches>("generatePatches") {
        group = taskGroup
        val sourceJar = getByName<Jar>("sourceJarW-ODep")
        var outputDir = getPatchesDirectory()
        dependsOn(sourceJar, "applyStyle")
        base = processedJar
        modified = sourceJar.archiveFile.get().asFile
        output = outputDir
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
    
    named("compileJava") {
        dependsOn("setup")
    }
    
    named("processResources") {
        dependsOn("extractResources")
    }
}

reobf {
    create("jar") {
        dependsOn("shadowJar")
    }
}

artifacts { 
    archives(tasks.getByName("sourceJar"))
}

repositories {
    mavenCentral()
    maven {
        name = "ic2"
        url = uri("https://maven.ic2.player.to/")
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
    
    ic2(group = "net.industrial-craft", name = "industrialcraft-2", version = "${versionIC2}-ex112", classifier = "dev")
    compileOnly(group = "mezz.jei", name = "jei_1.12.2", version = versionJEI)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-lib", version = versionBuildCraft)
    compileOnly(group = "com.mod-buildcraft", name = "buildcraft-main", version = versionBuildCraft)
    
    val ejml = create(group = "com.googlecode.efficient-java-matrix-library", name = "core", version = "0.26")
    implementation(ejml)
    shade(ejml)
}

open class ApplyAstyle : DefaultTask() {
    @get:InputFile
    val input: RegularFileProperty = this.project
            .objects
            .fileProperty()
    
    @get:OutputFile
    val output: RegularFileProperty = this.project
            .objects
            .fileProperty()
    
    @TaskAction
    fun execute() {
        val zipFile = ZipFile(input.get().asFile)
        val out = ZipOutputStream(FileOutputStream(output.get().asFile))
        
        for (entry in zipFile.entries()) {
            if (entry.name.startsWith("ic2")) {
                if (entry.name.endsWith(".java")) {
                    val txt = BufferedReader(InputStreamReader(zipFile.getInputStream(entry)))
                            .run {
                                val builder = StringBuilder()
                                forEachLine(builder::appendLine)
                                builder.toString()
                            }
                    val reader = StringReader(txt)

                    val newEntry = ZipEntry(entry.name)
                    out.putNextEntry(newEntry)

                    val outString = reader.readText().trimEnd() + System.lineSeparator()
                    out.write(outString.toByteArray())
                    out.closeEntry()
                } else if (entry.name.startsWith("ic2/profiles") || entry.name.startsWith("ic2/sounds")) {
                    val newEntry = ZipEntry(entry.name)
                    out.putNextEntry(newEntry)
                    out.write(zipFile.getInputStream(entry).readBytes())
                    out.closeEntry()
                }
            }
        }
        
        out.close()
    }
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
fun getPatchesDirectory(): File {
    project(":IC2-Base").projectDir.listFiles { _, name ->
        name.startsWith("patches[") && name.endsWith("]")
    }?.forEach { file ->
        val name = file.name.toString()
        val versions = name.substring(name.indexOf("[")+1, name.indexOf("]")).split(",")
        if (versions.size == 2) {
            if (compareVersions(versionIC2, versions[0])) {
                if (versions[1] == "+" || !compareVersions(versionIC2, versions[1])) {
                    return file("patches[${versions[0]},${versions[1]}]/minecraft")
                }
            }
        }
    }
    return File("patches/minecraft")
}
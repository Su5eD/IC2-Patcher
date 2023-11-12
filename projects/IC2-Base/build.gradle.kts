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
        patches = file("patches/minecraft")
        rejects = File(buildDir, "$name/rejects.zip")
        output = patchedJar
        
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
        
        from(zipTree(processedJarPatched))
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
        from(sourceSets.main.get().allJava)
    }
    
    register<TaskGeneratePatches>("generatePatches") {
        group = taskGroup
        val sourceJar = getByName<Jar>("sourceJar")
        val outputDir = file("patches/minecraft")
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
        dependsOn("classes")
    
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
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2855")
    
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
            if (entry.name.startsWith("ic2") && entry.name.endsWith(".java")) {
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
            }
        }
        
        out.close()
    }
}

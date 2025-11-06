/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package mods.su5ed.ic2patcher.asm;

import LZMA.LzmaInputStream;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import mods.su5ed.ic2patcher.util.IC2VersionExtractor;
import net.minecraftforge.fml.common.patcher.ClassPatch;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.repackage.com.nothome.delta.GDiffPatcher;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static mods.su5ed.ic2patcher.util.VersionComparison.compareVersions;

/**
 * Taken from MinecraftForge's {@link net.minecraftforge.fml.common.patcher.ClassPatchManager ClassPatchManager}
 * which is licensed under GNU Lesser General Public License version 2.1
 * @author MinecraftForge (Modified by Su5eD and Kanzaji)
 */
public class BinPatchManager {
    //Must be ABOVE INSTANCE so they get set in time for the constructor.
    public static final boolean dumpPatched = Boolean.parseBoolean(System.getProperty("fml.dumpPatchedClasses", "false"));
    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("fml.debugClassPatchManager", "false"));
    public static final Logger LOG = LogManager.getLogger("IC2-Patch-Manager");
    public static final BinPatchManager INSTANCE = new BinPatchManager();
    private final GDiffPatcher patcher = new GDiffPatcher();
    private ListMultimap<String, ClassPatch> patches;
    private final Map<String,byte[]> patchedClasses = Maps.newHashMap();
    private File tempDir;
    
    private BinPatchManager() {
        if (dumpPatched) {
            tempDir = Files.createTempDir();
            LOG.info("Dumping patched classes to {}",tempDir.getAbsolutePath());
        }
    }

    public byte[] applyPatch(String name, String mappedName, byte[] inputData) {
        if (patches == null) return inputData;
        if (patchedClasses.containsKey(name)) return patchedClasses.get(name);
        List<ClassPatch> list = patches.get(name);
        if (list.isEmpty()) return inputData;
        boolean ignoredError = false;
        if (DEBUG) LOG.debug("Runtime patching class {} (input size {}), found {} patch{}", mappedName, (inputData == null ? 0 : inputData.length), list.size(), list.size()!=1 ? "es" : "");
        for (ClassPatch patch: list) {
            if (!patch.targetClassName.equals(mappedName) && !patch.sourceClassName.equals(name)) {
                LOG.warn("Binary patch found {} for wrong class {}", patch.targetClassName, mappedName);
            }
            if (!patch.existsAtTarget && (inputData == null || inputData.length == 0)) {
                inputData = new byte[0];
            }
            else if (!patch.existsAtTarget) {
                LOG.warn("Patcher expecting empty class data file for {}, but received non-empty", patch.targetClassName);
            }
            else if (patch.existsAtTarget && (inputData == null || inputData.length == 0)) {
                LOG.fatal("Patcher expecting non-empty class data file for {}, but received empty.", patch.targetClassName);
                throw new RuntimeException(String.format("Patcher expecting non-empty class data file for %s, but received empty, your vanilla jar may be corrupt.", patch.targetClassName));
            }
            else {
                int inputChecksum = Hashing.adler32().hashBytes(inputData).asInt();
                if (patch.inputChecksum != inputChecksum) {
                    LOG.fatal("There is a binary discrepancy between the expected input class {} ({}) and the actual class. Checksum on disk is {}, in patch {}. Things are probably about to go very wrong. Did you put something into the jar file?", mappedName, name, Integer.toHexString(inputChecksum), Integer.toHexString(patch.inputChecksum));
                    if (!Boolean.parseBoolean(System.getProperty("fml.ignorePatchDiscrepancies","false"))) {
                        LOG.fatal("The game is going to exit, because this is a critical error, and it is very improbable that the modded game will work, please obtain clean jar files.");
                        System.exit(1);
                    }
                    else {
                        LOG.fatal("FML/IC2-Patcher is going to ignore this error, note that the patch will not be applied, and there is likely to be a malfunctioning behaviour, including not running at all");
                        ignoredError = true;
                        continue;
                    }
                }
            }
            synchronized (patcher) {
                try {
                    inputData = patcher.patch(inputData, patch.patch);
                }
                catch (IOException e) {
                    LOG.error("Encountered problem runtime patching class {}", name, e);
                    continue;
                }
            }
        }
        if (!ignoredError) LOG.info("Successfully applied runtime patches for {} (new size {})", mappedName, inputData.length);
        if (dumpPatched) {
            try {
                Files.write(inputData, new File(tempDir,mappedName));
            }
            catch (IOException e) {
                LOG.error(LOG.getMessageFactory().newMessage("Failed to write {} to {}", mappedName, tempDir.getAbsolutePath()), e);
            }
        }
        patchedClasses.put(name,inputData);
        return inputData;
    }

    public void setup(Map<String, Object> data) {
        File modJar = (File) data.get("coremodLocation");
        File mcLocation = (File) data.get("mcLocation");

        Pattern binpatchMatcher = Pattern.compile(String.format("binpatch/%s/.*.binpatch", "merged"));
        JarInputStream jis = null;
        try {
            try (ZipFile zip = new ZipFile(modJar)) {
                // Patches selection depending on the IC2 version.
                Enumeration<? extends ZipEntry> entries = zip.entries();
                String patches = "patches.pack.lzma";
                String versionIC2 = IC2VersionExtractor.getIC2Version(mcLocation);
                if (versionIC2 == null) {
                    if (!FMLLaunchHandler.isDeobfuscatedEnvironment()) throw new NullPointerException("Couldn't find IC2 Version! Is IC2 installed?");
                    LOG.error("Detected Deobfuscated Environment. IC2-Version extraction failure is going to be suppressed and no patches will be loaded.");
                    return;
                }
                if (DEBUG) LOG.debug("Current IC2 version: {}", versionIC2);
                versionIC2 = versionIC2.substring(0, versionIC2.indexOf("-"));

                while (entries.hasMoreElements()) {
                    // As you can't list all entries under x entry, like with a normal directory,
                    // it's required to search all entries of the Jar.
                    String name = entries.nextElement().getName();
                    if (!name.startsWith("patches/[") || !name.endsWith("]/patches.pack.lzma")) continue;

                    // Checking if IC2 Version is in the range specified by the Entry version range.
                    String[] versions = name.substring(name.indexOf("[")+1, name.indexOf("]")).split(",");
                    if (DEBUG) LOG.debug("Found patches for IC2 version: " + Arrays.toString(versions));

                    if (
                        (versions.length == 2 && compareVersions(versionIC2, versions[0]) && (Objects.equals(versions[1], "+") || compareVersions(versions[1], versionIC2))) ||
                        (versions.length == 1 && versions[0].equals(versionIC2))
                    ) {
                        if (DEBUG) LOG.debug("Loading patches from " + name + " for IC2 versions " + Arrays.toString(versions));
                        patches = name;
                        break;
                    }
                }

                InputStream binpatchesCompressed = zip.getInputStream(zip.getEntry(patches));
                if (binpatchesCompressed==null) {
                    if (!FMLLaunchHandler.isDeobfuscatedEnvironment()) {
                        LOG.fatal("The binary patch set is missing for your version of IC2, things are not going to work!");
                    }
                    return;
                }
                try (LzmaInputStream binpatchesDecompressedLzma = new LzmaInputStream(binpatchesCompressed)) {
                    byte[] decompressed = ByteStreams.toByteArray(binpatchesDecompressedLzma);
                    try (ByteArrayInputStream binpatchesDecompressed = new ByteArrayInputStream(decompressed)){
                        jis = new JarInputStream(binpatchesDecompressed);
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Error occurred reading binary patches. Expect severe problems!", e);
            }

            patches = ArrayListMultimap.create();

            do {
                try {
                    JarEntry entry = jis.getNextJarEntry();
                    if (entry == null) {
                        break;
                    }
                    if (binpatchMatcher.matcher(entry.getName()).matches()) {
                        ClassPatch patch = readPatch(entry, jis);
                        if (patch != null && patch.patch.length > 0) {
                            patches.put(patch.sourceClassName, patch);
                        }
                    }
                    else {
                        jis.closeEntry();
                    }
                }
                catch (IOException e) {
                    break;
                }
            } while (true);
        }
        finally {
            IOUtils.closeQuietly(jis);
        }
        LOG.debug("Read {} binary patches", patches.size());
        if (DEBUG) LOG.debug("Patch list :\n\t{}", Joiner.on("\t\n").join(patches.asMap().entrySet()));
        patchedClasses.clear();
    }

    private ClassPatch readPatch(JarEntry patchEntry, JarInputStream jis) {
        if (DEBUG) LOG.trace("Reading patch data from {}", patchEntry.getName());
        ByteArrayDataInput input;
        try {
            input = ByteStreams.newDataInput(ByteStreams.toByteArray(jis));
        }
        catch (IOException e) {
            LOG.warn(LOG.getMessageFactory().newMessage("Unable to read binpatch file {} - ignoring", patchEntry.getName()), e);
            return null;
        }
        int version = input.readByte(); // Default 1
        String name = input.readUTF();
        String sourceClassName = name.replace('/', '.');
        String targetClassName = input.readUTF().replace('/', '.');
        boolean exists = input.readBoolean();
        int inputChecksum = 0;
        if (exists) {
            inputChecksum = input.readInt();
        }
        int patchLength = input.readInt();
        byte[] patchBytes = new byte[patchLength];
        input.readFully(patchBytes);

        return new ClassPatch(name, sourceClassName, targetClassName, exists, inputChecksum, patchBytes);
    }
}

package mods.su5ed.ic2patcher.util;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("all")
public class IC2VersionExtractor {

    private static String ic2Version = null;
    private static final Logger logger = LogManager.getLogger("IC2-Patcher/IC2VersionExtraction");

    /**
     * Used to get cached IC2 Version extracted from the IC2 Jar. Requires {@link IC2VersionExtractor#getIC2Version(File)} to be called at least once.
     * @return String with IC2 Version.
     */
    public static String getIc2Version() throws IOException {return getIC2Version(null);}

    /**
     * Used to get IC2 Version from the jar itself. If was called at least once, returns cached value.
     * @param mcLocation File referencing MC Directory. If called more than once, can be null or {@link IC2VersionExtractor#getIC2Version()} can be used.
     * @return String with IC2 Version extracted, or null if not found.
     * @throws IOException when IO Exception occurs.
     */
    public static String getIC2Version(File mcLocation) throws IOException {
        if (mcLocation == null || ic2Version != null) return ic2Version;

        File mods = new File(mcLocation, "mods");

        if (!mods.exists() || mods.listFiles() == null) {
            logger.fatal("No mods folder exists or list of files returned is null! This error will cause a crash.");
            return null;
        }

        Function<File, String> searcher = (modsDir) -> {
            for (File file : modsDir.listFiles()) {
                if (!file.exists() || file.isDirectory() || !file.getName().endsWith(".jar")) continue;

                try (ZipFile zip = new ZipFile(file)) {
                    ZipEntry mcModInfoEntry = zip.getEntry("mcmod.info");
                    if (mcModInfoEntry == null) continue;
                    try {
                        // Unchecked Cast Warning*
                        Map<?,?> mcModInfo = ((List<Map<?,?>>)new Gson().fromJson(new InputStreamReader(zip.getInputStream(mcModInfoEntry)), Object.class)).get(0);
                        if (!Objects.equals(mcModInfo.get("modid"), "ic2")) continue;
                        ic2Version = (String) mcModInfo.get("version");
                        logger.info("IC2 was found! Extracted version: " + ic2Version);
                        return ic2Version;
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }
            return null;
        };

        String ver = searcher.apply(mods);
        if (ver != null) return ver;

        mods = new File(mods, "1.12.2");
        if (mods.exists() && mods.isDirectory()) {
            ver = searcher.apply(mods);
            if (ver != null) return ver;
        }

        logger.fatal("No IC2 was found in the mods folder. Is IC2 Installed? This error will cause a crash.");
        return null;
    }
}

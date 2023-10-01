package mods.su5ed.ic2patcher.util;

import com.google.gson.Gson;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("all")
public class IC2VersionExtractor {

    private static String ic2Version = null;

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
        if (mcLocation == null) return ic2Version;

        File mods = new File(mcLocation, "mods");

        if (!mods.exists() || mods.listFiles() == null) return null;

        for (File file : mods.listFiles()) {
            if (!file.exists() || file.isDirectory() || !file.getName().endsWith(".jar")) continue;

            try (ZipFile zip = new ZipFile(file)) {
                ZipEntry mcModInfoEntry = zip.getEntry("mcmod.info");
                if (mcModInfoEntry == null) continue;
                try {
                    // Unchecked Cast Warning*
                    Map<?,?> mcModInfo = ((List<Map<?,?>>)new Gson().fromJson(new InputStreamReader(zip.getInputStream(mcModInfoEntry)), Object.class)).get(0);
                    if (!Objects.equals(mcModInfo.get("modid"), "ic2")) continue;
                    ic2Version = (String) mcModInfo.get("version");
                    return ic2Version;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}

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

    /**
     * Used to get IC2 Version from the jar itself.
     * @param mcLocation File referencing MC Directory.
     * @return String with IC2 Version extracted, or null if not found.
     * @throws IOException when IO Exception occurs.
     */
    public static String getIC2Version(File mcLocation) throws IOException {
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
                    return (String) mcModInfo.get("version");
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}

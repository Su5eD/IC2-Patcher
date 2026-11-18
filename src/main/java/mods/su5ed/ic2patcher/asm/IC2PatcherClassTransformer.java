package mods.su5ed.ic2patcher.asm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import mods.su5ed.ic2patcher.asm.patches.IPatch;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static mods.su5ed.ic2patcher.asm.IC2PatcherCore.LOG;

public class IC2PatcherClassTransformer implements IClassTransformer {
    private static final Map<String, IPatch> customPatches = new HashMap<>();
    public static final List<String> loadedMods = new ArrayList<>();
    private final Map<String, byte[]> cache = new HashMap<>();

    public static void registerCustomPatch(@Nonnull IPatch patch) {
        if (!Objects.requireNonNull(patch, "Can't register a null patch!").isRequired(loadedMods)) {
            LOG.info("Skipping ASM patch \"{}\" for class \"{}\" because its requirements aren't met. Requires{}mods: {}",
                patch.getClass(), patch.getClassName(), patch.needsAllMods()? " ": " any of the ", patch.getRequiredMods()
            );
            return;
        }
        customPatches.put(patch.getClassName(), patch);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (customPatches.containsKey(name))
            return cache.computeIfAbsent(name, key -> customPatches.get(key).patch(key, transformedName, bytes));
        return IC2PatcherBinPatchManager.INSTANCE.applyPatch(name, transformedName, bytes);
    }

    public static void gatherMods() {
        Gson gson = new GsonBuilder().setLenient().create();

        try {
            Enumeration<URL> possibleMods = Launch.classLoader.getResources("mcmod.info");

            while (possibleMods.hasMoreElements()) {
                URL url = possibleMods.nextElement();
                if (url.getPath().contains("!/")) {
                    String filePath = url.getPath().split("!/")[0];
                    String[] split = filePath.split("/");
                    if (split.length != 0)
                        loadedMods.addAll(parseModInfo(gson, url));
                }
            }

            // Optifine is special.
            if (Objects.nonNull(Launch.classLoader.findResource("Config.class")))
                loadedMods.add("optifine");
        } catch (Exception e) {
            throw new RuntimeException("Failed early-discovery of mods!", e);
        }
    }

    @Nonnull
    private static List<String> parseModInfo(Gson gson, URL url) {
        try {
            List<String> mods = new ArrayList<>();
            JsonElement root = gson.fromJson(new InputStreamReader(url.openStream()), JsonElement.class);
            if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        mods.add(element.getAsJsonObject().get("modid").getAsString());
                    }
                }
            } else {
                for (JsonElement element : root.getAsJsonObject().get("modList").getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        mods.add(element.getAsJsonObject().get("modid").getAsString());
                    }
                }
            }

            return mods;
        } catch (Throwable e) {
            LOG.error("Failed parsing MCModInfo from {}!", url, e);
            return new ArrayList<>();
        }
    }
}

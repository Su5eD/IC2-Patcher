package mods.su5ed.ic2patcher.asm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import mods.su5ed.ic2patcher.asm.patches.UTLanguageSwitchPatch;
import mods.su5ed.ic2patcher.asm.patches.UTLanguageSwitchPatchTEST;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static mods.su5ed.ic2patcher.asm.IC2PatcherCore.LOG;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"mods.su5ed.ic2patcher.asm", "mods.su5ed.ic2patcher.asm.patches"})
@IFMLLoadingPlugin.SortingIndex(-1)
public class IC2PatcherFMLPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] { IC2PatcherClassTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return IC2PatcherCore.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        if (data.get("coremodLocation") == null || data.get("mcLocation") == null) return;
        IC2PatcherClassTransformer.gatherMods();
        IC2PatcherClassTransformer.registerCustomPatch(new UTLanguageSwitchPatch());
        IC2PatcherClassTransformer.registerCustomPatch(new UTLanguageSwitchPatchTEST());
        IC2PatcherBinPatchManager.INSTANCE.setup(data);
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

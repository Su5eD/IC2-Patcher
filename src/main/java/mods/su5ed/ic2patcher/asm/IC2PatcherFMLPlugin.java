package mods.su5ed.ic2patcher.asm;

import mods.su5ed.ic2patcher.asm.patches.UTLanguageSwitchPatch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.*;

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

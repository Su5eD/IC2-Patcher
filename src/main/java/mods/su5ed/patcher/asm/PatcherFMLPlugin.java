package mods.su5ed.patcher.asm;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("mods.su5ed.patcher.asm")
@IFMLLoadingPlugin.SortingIndex(-1)
public class PatcherFMLPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] { PatcherClassTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return PatcherDummyModContainer.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Object jar = data.get("coremodLocation");
        if (jar == null) return;
        BinPatchManager.INSTANCE.setup((File) jar);
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

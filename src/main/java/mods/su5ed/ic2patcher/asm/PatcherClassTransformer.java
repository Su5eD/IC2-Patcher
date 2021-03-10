package mods.su5ed.ic2patcher.asm;

import net.minecraft.launchwrapper.IClassTransformer;

public class PatcherClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        return BinPatchManager.INSTANCE.applyPatch(name, transformedName, bytes);
    }
}

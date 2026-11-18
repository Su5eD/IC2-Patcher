package mods.su5ed.ic2patcher.asm.patches;

import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.util.LogCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Patches List Click if UniversalTweaks is loaded, to fix IC2 Localization not reloading.
 */
@Patch(value = "net.minecraft.client.gui.GuiLanguage$List", mod = "universaltweaks")
public class UTLanguageSwitchPatch extends MethodPatch {

    public UTLanguageSwitchPatch() {
        super("elementClicked", "func_148144_a", "a",  "(IZII)V");
    }

    @Override
    public void patchMethod(ClassNode node, MethodNode method) {
        AbstractInsnNode ins = method.instructions.getFirst();
        while (ins != null) {
            if (ins.getOpcode() == Opcodes.RETURN) {
                method.instructions.insertBefore(ins, new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "mods/su5ed/ic2patcher/asm/patches/UTLanguageSwitchPatch",
                    "reloadIC2Localization",
                    "()V",
                    false
                ));
            }
            ins = ins.getNext();
        }
    }

    // This gets triggered twice but that shouldn't be a problem.
    public static void reloadIC2Localization() {
        Map<String, String> tmpMap = new HashMap<>();
        Map<String, String> lmMap = Localization.getLanguageMapMap();
        Map<String, String> localeMap = Localization.getLocaleMap();
        Set<String> languages = new LinkedHashSet<>();
        languages.add("en_us");
        languages.add(Minecraft.getMinecraft().gameSettings.language);

        for (String lang : languages) {
            try {
                for (IResource res : Minecraft.getMinecraft().getResourceManager().getAllResources(new ResourceLocation("ic2", Localization.getLangPath(lang)))) {
                    try {
                        tmpMap.clear();
                        Localization.loadLocalization(res.getInputStream(), tmpMap);
                        lmMap.putAll(tmpMap);
                        localeMap.putAll(tmpMap);
                        IC2.log.debug(LogCategory.Resource, "Loaded translation keys from %s.", res.getResourceLocation());
                    } finally {
                        try {
                            res.close();
                        } catch (IOException ignored) {}
                    }
                }
            } catch (FileNotFoundException ignored) {
                IC2.log.debug(LogCategory.Resource, "No translation file for language %s.", lang);
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
    }
}

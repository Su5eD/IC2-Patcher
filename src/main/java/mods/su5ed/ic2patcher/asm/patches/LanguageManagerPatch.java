package mods.su5ed.ic2patcher.asm.patches;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

import static mods.su5ed.ic2patcher.asm.BinPatchManager.LOG;

@Patch(className = "net.minecraft.client.resources.LanguageManager")
public class LanguageManagerPatch implements IPatch {

    // Part of the fix for #36
    //TODO: Think of moving the method handling to the annotation, providing the target method as AT target,
    // with just class name as fallback if more control is required.
    @Override
    public byte[] patch(String name, String mappedName, byte[] data) {
        ClassReader reader = new ClassReader(data);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        for (MethodNode method : node.methods) {
            if ((!Objects.equals(method.name, "func_110549_a") && !Objects.equals(method.name, "onResourceManagerReload"))
                || !Objects.equals(method.desc, "(Lnet/minecraft/client/resources/IResourceManager;)V"))
                continue;

            method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "ic2/core/init/Localization",
                "reloadLanguage",
                "()v",
                false
            ));
        }


        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] out = writer.toByteArray();
        LOG.info("Successfully applied runtime patches for {} (new size {})", mappedName, out.length);
        return out;
    }
}

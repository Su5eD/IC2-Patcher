package mods.su5ed.ic2patcher.asm.patches;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.Objects;

import static mods.su5ed.ic2patcher.asm.IC2PatcherCore.LOG;

public abstract class MethodPatch implements IPatch {
    private final String name;
    private final String srg;
    private final String obfs;
    private final String desc;

    public MethodPatch(String name, String srgName, String obfuscatedName, String desc) {
        this.name = name;
        this.srg = srgName;
        this.obfs = obfuscatedName;
        this.desc = desc;
    }

    @Override
    public byte[] patch(String name, String mappedName, byte[] data) {
        ClassReader reader = new ClassReader(data);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        for (MethodNode method : node.methods) {
            if ((!Objects.equals(method.name, this.name) && !Objects.equals(method.name, this.srg) && !Objects.equals(method.name, this.obfs))
                || !Objects.equals(method.desc, this.desc))
                continue;
            this.patchMethod(node, method);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] out = writer.toByteArray();
        LOG.info("Successfully applied runtime patches for {} (new size {})", mappedName, out.length);
        return out;
    }

    public abstract void patchMethod(ClassNode node, MethodNode method);
}

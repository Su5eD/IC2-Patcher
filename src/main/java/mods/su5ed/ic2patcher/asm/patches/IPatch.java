package mods.su5ed.ic2patcher.asm.patches;

public interface IPatch {
    byte[] patch(String name, String mappedName, byte[] data);
}

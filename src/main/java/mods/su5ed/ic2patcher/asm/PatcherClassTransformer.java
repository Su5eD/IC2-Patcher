package mods.su5ed.ic2patcher.asm;

import com.google.common.reflect.ClassPath;
import mods.su5ed.ic2patcher.IC2Patcher;
import mods.su5ed.ic2patcher.asm.patches.Patch;
import mods.su5ed.ic2patcher.asm.patches.IPatch;
import net.minecraft.launchwrapper.IClassTransformer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PatcherClassTransformer implements IClassTransformer {
    private final Map<String, IPatch> customPatches = new HashMap<>();

    @SuppressWarnings("all")
    public PatcherClassTransformer() {
        try {
            ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses().stream().forEach(info -> {
                Class<?> clazz = info.getClass();
                if (clazz.isAnnotationPresent(Patch.class)) {
                    if (!IPatch.class.isAssignableFrom(clazz)) {
                        IC2Patcher.logger.error("Class {} has \"Patch\" annotation without implementing the required interface! Skipping.", clazz.toString());
                        return;
                    }
                    Constructor<?>[] constructors = clazz.getConstructors();
                    Constructor<IPatch> patchConstructor = null;
                    for (Constructor<?> constructor : constructors) {
                        if (constructor.getParameterCount() != 0) continue;
                        patchConstructor = (Constructor<IPatch>) constructor;
                    }

                    if (Objects.isNull(patchConstructor)) {
                        IC2Patcher.logger.error("Patch {} doesn't provide a public non-arg constructor!", clazz.toString());
                    }

                    try {
                        customPatches.put(clazz.getDeclaredAnnotation(Patch.class).className(), patchConstructor.newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (customPatches.containsKey(name))
            return customPatches.get(name).patch(name, transformedName, bytes);
        return BinPatchManager.INSTANCE.applyPatch(name, transformedName, bytes);
    }
}

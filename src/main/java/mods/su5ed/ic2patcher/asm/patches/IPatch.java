package mods.su5ed.ic2patcher.asm.patches;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public interface IPatch {
    byte[] patch(String name, String mappedName, byte[] data);

    default String getClassName() {
        Patch annotation = this.getClass().getAnnotation(Patch.class);
        if (annotation.value().isEmpty()) throw new IllegalArgumentException("Invalid annotation for class " + this.getClass());
        return annotation.value();
    }

    default boolean isRequired(List<String> loadedMods) {
        if (!hasRequiredMods()) return true;
        List<String> mods = Arrays.asList(getRequiredMods());
        if (needsAllMods()) return new HashSet<>(loadedMods).containsAll(mods);
        return loadedMods.stream().anyMatch(mods::contains);
    }

    default boolean needsAllMods() {
        return this.getClass().getAnnotation(Patch.class).requireAll();
    }

    default String[] getRequiredMods() {
        return this.getClass().getAnnotation(Patch.class).mod();
    }

    default boolean hasRequiredMods() {
        return getRequiredMods().length > 0;
    }
}

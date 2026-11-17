package mods.su5ed.ic2patcher.asm.patches;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used for discovery of patches done "the old way". Annotated class has to be implementor of {@link IPatch}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {
    /**
     * Class name your {@link IPatch} modifies.
     * @return String representation of the class name to modify.
     */
    String className();
}

package mods.su5ed.ic2patcher.asm.patches;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//TODO: Add an option to define a mod version range for the patch dependency. For now, not necessary.
/**
 * Used for discovery of patches done "the old way". Annotated class has to be implementor of {@link IPatch}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {
    /**
     * Class name your {@link IPatch} modifies.
     * @return String representation of the class name to modify.
     */
    String value();

    /**
     * If specified, only applies the patch if the provided mod id(s) was loaded.
     * @return Mod ID required for this patch.
     */
    String[] mod() default {};

    /**
     * If true, all mod ids specified in {@link mod} are required for patch to be enabled.
     * @return True if all specified mod ids are required, false if only one.
     */
    boolean requireAll() default false;
}

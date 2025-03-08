package mods.su5ed.ic2patcher.compat;

import ic2.core.item.ItemClassicCell;
import ic2.core.item.type.CellType;
import ic2.core.ref.ItemName;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mods.su5ed.ic2patcher.IC2Patcher;
import net.minecraft.item.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@JEIPlugin
public class JeiModule implements IModPlugin {
    private static final MethodHandle INSTANCE_FIELD;
    private static final MethodHandle APPLY_HANDLE;

    static {
        MethodHandle applyHandle;
        MethodHandle instanceField;
        try {
            Class<?> classFluidSubtypeInterpreter = Class.forName("mezz.jei.plugins.vanilla.ingredients.item.ItemStackListFactory$FluidSubtypeInterpreter");
            Field fieldInstance = classFluidSubtypeInterpreter.getDeclaredField("INSTANCE");
            fieldInstance.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            instanceField = lookup.unreflectGetter(fieldInstance);

            Method methodApply = classFluidSubtypeInterpreter.getDeclaredMethod("apply", ItemStack.class);
            methodApply.setAccessible(true);
            applyHandle = lookup.unreflect(methodApply);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
            instanceField = applyHandle = null;
            IC2Patcher.logger.info("Failed to unreflect members of FluidSubtypeInterpreter, the JEI cell subtype patch won't work");
        }
        INSTANCE_FIELD = instanceField;
        APPLY_HANDLE = applyHandle;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        registry.registerSubtypeInterpreter(ItemName.cell.getInstance(), new CellSubtypeInterpreter());
    }

    private static class CellSubtypeInterpreter implements ISubtypeRegistry.ISubtypeInterpreter {
        @Override
        public String apply(ItemStack itemStack) {
            ItemClassicCell cell = ItemName.cell.getInstance();
            CellType type = cell.getType(itemStack);
            if (type != null) {
                if (type.isFluidContainer()) {
                    String name = getName(itemStack);
                    if (name != null) return name;
                } else return "m="+itemStack.getMetadata();
            }

            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        }

        private static String getName(ItemStack stack) {
            try {
                Object instance = INSTANCE_FIELD.invoke();
                return (String) APPLY_HANDLE.invoke(instance, stack);
            } catch (Throwable ignored) {}

            return null;
        }
    }
}

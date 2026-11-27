package mods.su5ed.ic2patcher.ic2.recipes;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.core.IC2;
import ic2.core.util.Config;
import ic2.core.util.LogCategory;
import mods.su5ed.ic2patcher.ic2.util.IC2ConfigUtil;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static ic2.core.init.Rezepte.splitWhitespace;

public class ElectrolyzerRecipes {
    static boolean loadRecipe(Config.Value value, IElectrolyzerRecipeManager manager, boolean lastAttempt) {
        List<IElectrolyzerRecipeManager.ElectrolyzerOutput> output = new ArrayList<>();
        FluidStack input;
        int duration = 200;
        int EUaTick = -1;

        try {
            input = IC2ConfigUtil.getFluidStack(value.name, true);
        } catch (Exception ex) {
            throw new Config.ParseException("invalid key", value, ex);
        }

        if (input == null) {
            if (lastAttempt) {
                IC2.log.warn(
                    LogCategory.Recipe,
                    new Config.ParseException("invalid input specified: " + value.name, value),
                    "Skipping recipe due to unresolvable input %s.",
                    value.name
                );
            } else {
                Rezepte.pendingRecipes.add(() -> loadRecipe(value, manager, true));
            }

            return false;
        }

        try {
            for (String part : splitWhitespace(value.value)) {
                if (part.startsWith("@")) {
                    if (part.startsWith("@duration:")) {
                        duration = Integer.parseInt(part.substring(10));
                        continue;
                    }
                    if (part.startsWith("@energy:")) {
                        EUaTick = Integer.parseInt(part.substring(8));
                        continue;
                    }
                    throw new Config.ParseException("invalid attribute: " + part, value);
                }

                IElectrolyzerRecipeManager.ElectrolyzerOutput out = getOutput(part);
                if (out == null) {
                    if (lastAttempt) {
                        IC2.log.warn(
                            LogCategory.Recipe,
                            new Config.ParseException("invalid output specified", value),
                            "Skipping recipe using %s due to unresolvable output.",
                            value.name
                        );
                    } else {
                        Rezepte.pendingRecipes.add(() -> loadRecipe(value, manager, true));
                    }

                    return false;
                }

                // This can't be respected because IRecipeInput is item-only input/output, and we output FluidStack!
//                for (IRecipeInput disable : disabledRecipeOutputs) {
//                    if (disable.matches(output)) {
//                        return true;
//                    }
//                }

                output.add(out);
            }
        } catch (Exception e) {
            throw new Config.ParseException("Invalid output ", value, e);
        }

        if (EUaTick < 0)
            throw new Config.ParseException("Missing @energy attribute!", value);

        try {
            // Why this recipe manager *throws* on duplicate instead of providing a boolean is beyond my understanding.
            manager.addRecipe(input.getFluid().getName(), input.amount, EUaTick, duration, output.toArray(new IElectrolyzerRecipeManager.ElectrolyzerOutput[]{}));
            return true;
        } catch (RuntimeException ex) {
            throw new Config.ParseException("Failed recipe validation!", value, ex);
        }
    }

    /**
     * Modified {@link IC2ConfigUtil#getFluidStack(String, boolean)} for parsing of @direction attribute.
     * @param str String to parse
     * @return Electrolyzer Output, or null if failed to parse.
     */
    private static IElectrolyzerRecipeManager.ElectrolyzerOutput getOutput(String str) throws ParseException {
        String[] parts = str.split("(?=(@|#|\\*))");
        String fluidName = parts[0];
        if (!fluidName.startsWith("Fluid:")) return null;
        Integer amount = null;
        EnumFacing dir = null;
        for (int i = 1; i < parts.length; i++) {
            String tmp = parts[i];

            if (tmp.startsWith("*"))
                amount = Integer.parseInt(tmp.substring(1));

            if (tmp.startsWith("@")) {
                if (dir != null)
                    throw new ParseException("Multiple direction attributes are not supported here!", -1);
                dir = EnumFacing.byName(tmp.substring(1));
            }
        }

        if (dir == null) throw new ParseException("Direction attribute is required here!", -1);
        if (amount == null) amount = 1000;
        Fluid fluid = FluidRegistry.getFluid(fluidName.substring(6));
        if (fluid == null) return null;
        return new IElectrolyzerRecipeManager.ElectrolyzerOutput(fluid.getName(), amount, dir);
    }
}

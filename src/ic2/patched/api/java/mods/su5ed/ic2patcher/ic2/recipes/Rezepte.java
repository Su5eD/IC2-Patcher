package mods.su5ed.ic2patcher.ic2.recipes;

import ic2.api.recipe.*;
import ic2.core.IC2;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import mods.su5ed.ic2patcher.IC2PatcherConfig;
import mods.su5ed.ic2patcher.ic2.util.IC2ConfigUtil;
import mods.su5ed.ic2patcher.util.IC2VersionExtractor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.text.ParseException;
import java.util.*;
import java.util.function.BooleanSupplier;

import static ic2.core.init.Rezepte.getConfigFile;
import static ic2.core.init.Rezepte.splitWhitespace;

/**
 * Handles loading of recipes for machines without official .ini support.<br>
 * Resolves <a href="https://github.com/Su5eD/IC2-Patcher/issues/43">#43</a>
 */
public class Rezepte {
    private static List<IRecipeInput> disabledRecipeOutputs = null;
    private static Queue<BooleanSupplier> pendingRecipes = null;

    public static void loadAdditionalRecipes(List<IRecipeInput> disabledRecipes, Queue<BooleanSupplier> pendingRecipes) {
        Rezepte.disabledRecipeOutputs = disabledRecipes;
        Rezepte.pendingRecipes = pendingRecipes;

        Config cannerEnrich = new Config("canner enrichment recipes");
        Config cannerBottle = new Config("canner bottling recipes");

        try {
            cannerEnrich.load(getConfigFile("canner_enrich"));
            cannerBottle.load(getConfigFile("canner_bottle"));
        } catch (Exception e) {
            IC2.log.warn(LogCategory.Recipe, e, "Recipe loading failed.");
        }

        // 2.2.164 IC2 Compatiblity. It doesn't contain milk!
        if (IC2VersionExtractor.checkVersion("2.8.164-ex112"))
            cannerEnrich.remove("Fluid:water ic2:dust#milk");

        loadEnrichRecipes(cannerEnrich, Recipes.cannerEnrich);
        loadBottleRecipes(cannerBottle, Recipes.cannerBottle);
    }

    private static void loadEnrichRecipes(Config config, ICannerEnrichRecipeManager machine) {
        int amount = 0;
        int successful = 0;

        for (Iterator<Config.Value> it = config.valueIterator(); it.hasNext(); amount++) {
            Config.Value value = it.next();
            if (loadEnrichRecipe(value, machine, false)) {
                successful++;
            }
        }

        IC2.log.info(LogCategory.Recipe, "Successfully loaded " + successful + " out of " + amount + " recipes for " + config.name);
    }

    private static void loadBottleRecipes(Config config, ICannerBottleRecipeManager machine) {
        int amount = 0;
        int successful = 0;

        for (Iterator<Config.Value> it = config.valueIterator(); it.hasNext(); amount++) {
            Config.Value value = it.next();
            if (loadBottleRecipe(value, machine, false)) {
                successful++;
            }
        }

        IC2.log.info(LogCategory.Recipe, "Successfully loaded " + successful + " out of " + amount + " recipes for " + config.name);
    }

    private static boolean loadEnrichRecipe(Config.Value value, ICannerEnrichRecipeManager machine, boolean lastAttempt) {
        IRecipeInput additive;
        FluidStack output;
        FluidStack input;
        List<String> split = splitWhitespace(value.name);
        if (split.size() != 2)
            throw new Config.ParseException("Invalid input + additive pair", value);

        String inputStr = split.get(0).trim();
        String additiveStr = split.get(1).trim();

        try {
            input = IC2ConfigUtil.getFluidStack(inputStr, true);

            if (additiveStr.contains("@exact")) {
                additive = Recipes.inputFactory.forExactStack(ConfigUtil.asStackWithAmount(additiveStr.replace("@exact", "").trim()));
            } else {
                additive = ConfigUtil.asRecipeInputWithAmount(additiveStr);
            }
        } catch (Exception ex) {
            throw new Config.ParseException("invalid key", value, ex);
        }

        if (input == null || additive == null) {
            if (lastAttempt) {
                IC2.log.warn(
                    LogCategory.Recipe,
                    new Config.ParseException("invalid input specified: " + value.name, value),
                    "Skipping recipe due to unresolvable input%s%s.",
                    input == null? " " + inputStr: "",
                    additive == null? " " + additiveStr: ""
                );
            } else {
                pendingRecipes.add(() -> loadEnrichRecipe(value, machine, true));
            }

            return false;
        }

        try {
            output = IC2ConfigUtil.getFluidStack(value.value, true);
            if (output == null) {
                if (lastAttempt) {
                    IC2.log.warn(
                        LogCategory.Recipe,
                        new Config.ParseException("invalid output specified", value),
                        "Skipping recipe using %s due to unresolvable output.",
                        value.name
                    );
                } else {
                    pendingRecipes.add(() -> loadEnrichRecipe(value, machine, true));
                }

                return false;
            }

            // This can't be respected because IRecipeInput is item-only input/output, and we output FluidStack!
//            for (IRecipeInput disable : disabledRecipeOutputs) {
//                if (disable.matches(output)) {
//                    return true;
//                }
//            }
        } catch (Exception e) {
            throw new Config.ParseException("Invalid output ", value, e);
        }

        if (machine.addRecipe(new ICannerEnrichRecipeManager.Input(input, additive), output, null, false)) {
            return true;
        } else {
            throw new Config.ParseException("Conflicting recipe", value);
        }
    }

    private static boolean loadBottleRecipe(Config.Value value, ICannerBottleRecipeManager machine, boolean lastAttempt) {
        IRecipeInput container;
        IRecipeInput fill;
        ItemStack output;
        List<String> split = splitWhitespace(value.name);
        if (split.size() != 2)
            throw new Config.ParseException("Invalid input + additive pair", value);

        String containerStr = split.get(0).trim();
        String fillStr = split.get(1).trim();
        try {
            if (containerStr.contains("@exact")) {
                container = Recipes.inputFactory.forExactStack(ConfigUtil.asStackWithAmount(containerStr.replace("@exact", "").trim()));
            } else {
                container = ConfigUtil.asRecipeInputWithAmount(containerStr);
            }

            if (fillStr.contains("@exact")) {
                fill = Recipes.inputFactory.forExactStack(ConfigUtil.asStackWithAmount(fillStr.replace("@exact", "").trim()));
            } else {
                fill = ConfigUtil.asRecipeInputWithAmount(fillStr);
            }
        } catch (Exception ex) {
            throw new Config.ParseException("invalid key", value, ex);
        }

        if (container == null || fill == null) {
            if (lastAttempt) {
                IC2.log.warn(
                    LogCategory.Recipe,
                    new Config.ParseException("invalid input specified: " + value.name, value),
                    "Skipping recipe due to unresolvable input%s%s.",
                    container == null? " " + containerStr: "",
                    fill == null? " " + fillStr: ""
                );
            } else {
                pendingRecipes.add(() -> loadBottleRecipe(value, machine, true));
            }

            return false;
        }

        try {
            output = ConfigUtil.asStackWithAmount(value.value);

            if (output == null) {
                if (lastAttempt) {
                    IC2.log.warn(
                        LogCategory.Recipe,
                        new Config.ParseException("invalid output specified", value),
                        "Skipping recipe using %s due to unresolvable output.",
                        value.name
                    );
                } else {
                    pendingRecipes.add(() -> loadBottleRecipe(value, machine, true));
                }

                return false;
            }

            for (IRecipeInput disable : disabledRecipeOutputs) {
                if (disable.matches(output)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new Config.ParseException("generic parse error", value, e);
        }

        if (machine.addRecipe(new ICannerBottleRecipeManager.Input(container, fill), output, null, false)) {
            return true;
        } else {
            throw new Config.ParseException("Conflicting recipe", value);
        }
    }
}

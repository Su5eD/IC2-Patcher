package mods.su5ed.ic2patcher.ic2.recipes;

import ic2.api.recipe.*;
import ic2.core.IC2;
import ic2.core.util.Config;
import ic2.core.util.LogCategory;
import mods.su5ed.ic2patcher.util.IC2VersionExtractor;
import mods.su5ed.ic2patcher.util.TriFunction;

import java.util.*;
import java.util.function.BooleanSupplier;

import static ic2.core.init.Rezepte.getConfigFile;

/**
 * Handles loading of recipes for machines without official .ini support.<br>
 * Resolves <a href="https://github.com/Su5eD/IC2-Patcher/issues/43">#43</a>
 */
public class Rezepte {
    static List<IRecipeInput> disabledRecipeOutputs = null;
    static Queue<BooleanSupplier> pendingRecipes = null;

    public static void loadAdditionalRecipes(List<IRecipeInput> disabledRecipes, Queue<BooleanSupplier> pendingRecipes) {
        Rezepte.disabledRecipeOutputs = disabledRecipes;
        Rezepte.pendingRecipes = pendingRecipes;

        Config cannerEnrich = new Config("canner enrichment recipes");
        Config cannerBottle = new Config("canner bottling recipes");
        Config electrolyzer = new Config("electrolyzer recipes");

        try {
            cannerEnrich.load(getConfigFile("canner_enrich"));
            cannerBottle.load(getConfigFile("canner_bottle"));
            electrolyzer.load(getConfigFile("electrolyzer"));
        } catch (Exception e) {
            IC2.log.warn(LogCategory.Recipe, e, "Recipe loading failed.");
        }

        // 2.2.164 IC2 Compatiblity. It doesn't contain milk!
        if (IC2VersionExtractor.checkVersion("2.8.164-ex112"))
            cannerEnrich.remove("Fluid:water ic2:dust#milk");

        loadRecipes(cannerEnrich, Recipes.cannerEnrich, CannerRecipes::loadEnrichRecipe);
        loadRecipes(cannerBottle, Recipes.cannerBottle, CannerRecipes::loadBottleRecipe);
        loadRecipes(electrolyzer, Recipes.electrolyzer, ElectrolyzerRecipes::loadRecipe);
    }

    static <M> void loadRecipes(Config config, M recipeManager, TriFunction<Config.Value, M, Boolean, Boolean> loader) {
        int amount = 0;
        int successful = 0;

        for (Iterator<Config.Value> it = config.valueIterator(); it.hasNext(); amount++) {
            Config.Value value = it.next();
            if (loader.apply(value, recipeManager, false)) {
                successful++;
            }
        }

        IC2.log.info(LogCategory.Recipe, "Successfully loaded " + successful + " out of " + amount + " recipes for " + config.name);
    }
}

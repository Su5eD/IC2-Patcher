package mods.su5ed.ic2patcher;

import ic2.api.item.IC2Items;
import ic2.core.IC2;
import ic2.core.WorldData;
import ic2.core.util.StackUtil;
import mods.su5ed.ic2patcher.util.RecipeUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import org.apache.logging.log4j.Logger;

@Mod(modid = "mods/su5ed/ic2patcher", name = "IC2 Patcher", dependencies = "required-after:ic2@[2.8.164-ex112],[2.8.221-ex112,);")
public final class IC2Patcher {
    public static Logger logger;

    @Mod.EventHandler
    public void start(FMLConstructionEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        IC2PatcherConfig.get();
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        if (IC2.version.isClassic()) fixUraniumCellRecipe();
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {}

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {}

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (!IC2PatcherConfig.get().worldReferenceSwitch) WorldData.resetMaps();
    }

    private static void fixUraniumCellRecipe() {
        ItemStack cell = IC2Items.getItem("cell", "empty");
        IRecipe recipe = RecipeUtil.getCraftingRecipe(cell, cell, cell, cell, IC2Items.getItem("ingot", "uranium"), cell, cell, cell, cell);
        if (recipe != null) {
            ItemStack output = recipe.getRecipeOutput();
            if (output.isItemEqual(IC2Items.getItem("nuclear", "near_depleted_uranium")) && output.getCount() == 1) {
                ((IForgeRegistryModifiable<?>) ForgeRegistries.RECIPES).remove(recipe.getRegistryName());
                GameRegistry.addShapedRecipe(
                        recipe.getRegistryName(),
                        new ResourceLocation(recipe.getGroup()),
                        StackUtil.copyWithSize(output, 8),
                        "CCC", "CUC", "CCC", 'C', cell, 'U', "ingotUranium"
                );
            }
        }
    }
}

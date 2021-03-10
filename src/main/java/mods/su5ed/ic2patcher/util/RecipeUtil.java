package mods.su5ed.ic2patcher.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class RecipeUtil {

    public static IRecipe getCraftingRecipe(ItemStack... stacks) {
        InventoryCrafting crafting = new InventoryCrafting(new DummyContainer(),  3, 3);
        for (int i = 0; i < 9 && i < stacks.length; i++) {
            crafting.setInventorySlotContents(i, stacks[i]);
        }
        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            try {
                if (recipe.matches(crafting, DummyWorld.INSTANCE)) {
                    return recipe;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    }
}

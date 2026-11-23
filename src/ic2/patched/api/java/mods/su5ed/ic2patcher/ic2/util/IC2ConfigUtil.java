package mods.su5ed.ic2patcher.ic2.util;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.text.ParseException;

public class IC2ConfigUtil {
    public static FluidStack getFluidStack(String str, boolean checkAmount) throws ParseException {
        String[] parts = str.split("(?=(@|#|\\*))");
        String itemName = parts[0];
        if (!itemName.startsWith("Fluid:")) return null;
        Integer amount = null;
        for (int i = 1; i < parts.length; i++) {
            String tmp = parts[i];
            if (tmp.startsWith("*")) {
                if (!checkAmount) throw new ParseException("We do not support amount here.", -1);

                amount = Integer.parseInt(tmp.substring(1));
            }
        }

        if (itemName.startsWith("OreDict:")) return null;
        if (amount == null) amount = 1000;
        Fluid fluid = FluidRegistry.getFluid(itemName.substring("Fluid:".length()));
        if (fluid == null) return null;
        return new FluidStack(fluid, amount);
    }
}

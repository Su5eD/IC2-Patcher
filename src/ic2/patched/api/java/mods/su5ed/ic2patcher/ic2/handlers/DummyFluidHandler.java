package mods.su5ed.ic2patcher.ic2.handlers;

import ic2.core.init.BlocksItems;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Dummy Fluid Handler that doesn't accept any I/O.
 * @see BlocksItems#initItems()  BlocksItems#initItems() ItemClassicCell initialization.
 * @apiNote See <a href="https://github.com/Su5eD/IC2-Patcher/issues/26">IC2-Patcher/#26</a>
 */
public class DummyFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;

    public DummyFluidHandler(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[0];
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        return 0;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return null;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return null;
    }

    @Nonnull
    @Override
    public ItemStack getContainer() {
        return stack;
    }
}

package mods.su5ed.patcher.asm;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

public class PatcherDummyModContainer extends DummyModContainer {
    public PatcherDummyModContainer() {
        super(new ModMetadata());
        ModMetadata meta = super.getMetadata();
        meta.modId = "ic2patcher-core";
        meta.name = "IC2 Patcher Core";
        meta.version = "1.0";
        meta.authorList = Lists.newArrayList("Su5eD");
        meta.description = "IC2 Patcher Coremod";
        meta.screenshots = new String[0];
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }
}

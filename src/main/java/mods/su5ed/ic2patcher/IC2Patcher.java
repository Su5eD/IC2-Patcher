package mods.su5ed.ic2patcher;

import ic2.core.WorldData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = "ic2patcher",
    name = "IC2 Patcher",
    dependencies = "required-after:ic2@[2.8.164-ex112],[2.8.221-ex112,);",
    useMetadata = true
)
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
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (!IC2PatcherConfig.get().worldReferenceSwitch) WorldData.resetMaps();
    }
}

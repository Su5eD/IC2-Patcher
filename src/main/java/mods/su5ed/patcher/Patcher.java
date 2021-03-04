package mods.su5ed.patcher;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = "ic2patcher", name = "IC2 Patcher", dependencies = "required-before:ic2@[2.8.221-ex112,];")
public final class Patcher {
    public static Logger logger;

    @Mod.EventHandler
    public void start(FMLConstructionEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {}

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {}
}

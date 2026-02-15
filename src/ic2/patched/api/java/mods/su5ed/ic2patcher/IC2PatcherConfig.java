package mods.su5ed.ic2patcher;

import java.io.File;
import java.util.Objects;


public class IC2PatcherConfig extends net.minecraftforge.common.config.Configuration {

    private static IC2PatcherConfig config;
    public final boolean newUUCalculator;
    public final boolean worldReferenceSwitch;
    public final boolean enableUpgradeTickInterval;
    public final boolean bucketCell;
    public final boolean qsNVBuff;
    public final boolean nsFDBuff;
    public final int upgradeTickInterval;
    public final int pushCooldown;

    protected IC2PatcherConfig(String file) {
        super(new File("config/" + file + ".cfg"));

        bucketCell = this.getBoolean("bucketFluidCell", "patches", true,
            "Adds bucket-like behaviour to the FluidCell, allowing to pickup fluids that aren't adjacent to a block.");

        enableUpgradeTickInterval = this.getBoolean("enableUpgradeTickInterval", "patches", false,
            "[Experimental] Forces the Push/Pull upgrades to tick only once every x ticks with few exceptions. If false, uses default IC2 behaviour.");

        upgradeTickInterval = this.getInt("upgradeSlotTickInterval", "patches", 5, 1, 100,
            "[Experimental] Forces the Push/Pull upgrades to tick only once every x ticks with few exceptions.\n" +
                "- For Push: Ticked if output is full, if transfer failed, triggers a cooldown.\n" +
                "- For Pull: Tick is skipped if output/input is full.\n ");

        pushCooldown = this.getInt("pushUpgradeCooldown", "patches", 20, 1, 1200,
            "[Experimental] Determines cooldown (in ticks) of push upgrades, after failed item transfer.");

        worldReferenceSwitch = this.getBoolean("worldReferenceSwitch", "patches", false,
            "[Experimental] Changes the way EnergyNet handles and stores World Reference, to possibly fix randomly disconnecting wires.\n" +
                "Enabling this switches out the previous patch for this new solution. Please report any issues related to EnergyNet if you have this patch enabled.\n" +
                "Creating a backup of the world before turning this patch on is recommended.");

        newUUCalculator = this.getBoolean("newUUMatterCalculator", "patches", false,
            "[Experimental] Replaces the original UU-Matter calculator with a completely custom solution.\n" +
                "This will affect UU-Values being calculated, to be possibly more \"fair\" than the defaults," +
                " and should resolve compatibility issues with mods that cause crashes of original solution.");

        qsNVBuff = this.getBoolean("buffedQuantumSuitNV", "balance", true,
            "Makes QuantumSuit Night Vision fully automatic, removing the blindness effect if the surrounds are too bright, only removing the Night Vision effect in that situation.");

        nsFDBuff = this.getBoolean("buffedNanoSuitFallDamageReduction", "balance", true,
            "Makes NanoSuit absorb up to 8 fall damage, instead of applying the full fall damage if it's higher than 8.");

        if (this.hasChanged()) this.save();
    }

    public static IC2PatcherConfig get() {
        if (config == null) {
            set(new IC2PatcherConfig("IC2-Patcher"));
            config.load();
        }
        return config;
    }

    protected static void set(IC2PatcherConfig config) {
        IC2PatcherConfig.config = Objects.requireNonNull(config);
    }
}
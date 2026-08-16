package com.mrfuzzihead.fuzzitweaks;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Category used for Minecraft tweak settings. */
    public static final String CATEGORY_MINECRAFT = "minecraft";

    /** Category used for the Distant Horizons per-dimension LOD generation filter settings. */
    public static final String CATEGORY_DISTANT_HORIZONS = "distanthorizons";

    /** Category used for ProjectE tweaks settings. */
    public static final String CATEGORY_PROJECTE = "projecte";

    /** Category used for Thaumic Additions tweaks settings. */
    public static final String CATEGORY_THAUMIC_ADDITIONS = "thaumicadditions";

    /** Category used for Not Enough Items tweaks settings. */
    public static final String CATEGORY_NEI = "nei";

    /** Category used for MCPatcher tweaks settings. */
    public static final String CATEGORY_MCPATCHER = "mcpatcher";

    /** Category used for Galacticraft tweaks settings. */
    public static final String CATEGORY_GALACTICRAFT = "galacticraft";

    public static boolean enableDespawnModule = true;

    public static boolean enableBackgroundScreenshot = true;

    public static int maxMobBlockLightLevel = 0;

    public static int maxMobSkyLightLevel = 7;

    public static boolean enableProjectETweaks = true;

    public static boolean enableThaumicAdditionsTweaks = true;

    public static boolean enableNEITweaks = true;

    public static boolean enableMCPatcherTweaks = true;

    public static boolean enableGalacticraftCoalGeneratorFuelTweak = true;

    public static boolean enableGalacticraftCompressorNEIFix = true;

    public static boolean enableDistantHorizonsDimensionFilter = true;

    public static int[] distantHorizonsDimensionIds = new int[] { 0 };

    /**
     * If {@code true}, {@link #distantHorizonsDimensionIds} is treated as a denylist (every
     * dimension except the listed ones may generate LODs). If {@code false}, it's treated as an
     * allowlist (only the listed dimensions may generate LODs).
     */
    public static boolean distantHorizonsDimensionListIsDenylist = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        enableDespawnModule = configuration.getBoolean(
            "EnableDespawnModule",
            CATEGORY_MINECRAFT,
            true,
            "Enable module that lets mobs holding items/armor despawn");

        maxMobBlockLightLevel = configuration.getInt(
            "MaxMobSpawnLightLevel",
            CATEGORY_MINECRAFT,
            0,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using block light");

        maxMobSkyLightLevel = configuration.getInt(
            "MaxMobSkyLightLevel",
            CATEGORY_MINECRAFT,
            7,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using sky light");

        enableBackgroundScreenshot = configuration.getBoolean(
            "EnableBackgroundScreenshot",
            CATEGORY_MINECRAFT,
            true,
            "Take screenshots (F2) off the main thread so the game doesn't freeze while the PNG is written");

        enableProjectETweaks = configuration
            .getBoolean("EnableProjectETweaks", CATEGORY_PROJECTE, true, "Enable fixes and tweaks for ProjectE");

        enableThaumicAdditionsTweaks = configuration.getBoolean(
            "EnableThaumicAdditionsTweaks",
            CATEGORY_THAUMIC_ADDITIONS,
            true,
            "Enable fixes and tweaks for Thaumic Additions");

        enableNEITweaks = configuration
            .getBoolean("EnableNEITweaks", CATEGORY_NEI, true, "Enable fixes and tweaks for Not Enough Items");

        enableMCPatcherTweaks = configuration
            .getBoolean("EnableMCPatcherTweaks", CATEGORY_MCPATCHER, true, "Enable fixes and tweaks for MCPatcher");

        enableGalacticraftCoalGeneratorFuelTweak = configuration.getBoolean(
            "EnableGalacticraftCoalGeneratorFuelTweak",
            CATEGORY_GALACTICRAFT,
            true,
            "Allow the Galacticraft Coal Generator to burn any furnace-burnable item (charcoal, wood, etc.), "
                + "scaled to each item's vanilla furnace burn time.");

        enableGalacticraftCompressorNEIFix = configuration.getBoolean(
            "EnableGalacticraftCompressorNEIFix",
            CATEGORY_GALACTICRAFT,
            true,
            "Re-register GC's Ingot/Electric-Ingot Compressor NEI handlers. The GTNH Galacticraft fork comments "
                + "these out (it expects GregTech's Implosion Compressor to show them), so the GC Compressor "
                + "recipes never appear in NEI when GregTech isn't present.");

        enableDistantHorizonsDimensionFilter = configuration.getBoolean(
            "EnableDistantHorizonsDimensionFilter",
            CATEGORY_DISTANT_HORIZONS,
            true,
            "Enable the per-dimension allow/deny list for Distant Horizons LOD generation");

        distantHorizonsDimensionIds = configuration.get(
            CATEGORY_DISTANT_HORIZONS,
            "DimensionIds",
            new int[0],
            "Dimension IDs this list applies to, see DimensionListIsDenylist. Vanilla: 0=Overworld, -1=Nether, 1=End; "
                + "modded dimensions use their own IDs.")
            .getIntList();

        distantHorizonsDimensionListIsDenylist = configuration.getBoolean(
            "DimensionListIsDenylist",
            CATEGORY_DISTANT_HORIZONS,
            true,
            "If true, DimensionIds is a denylist: Distant Horizons generates LODs everywhere except those dimensions. "
                + "If false, DimensionIds is an allowlist: Distant Horizons only generates LODs in those dimensions.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}

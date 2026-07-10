package com.mrfuzzihead.fuzzitweaks;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Category used for the Distant Horizons per-dimension LOD generation filter settings. */
    public static final String CATEGORY_DISTANT_HORIZONS = "distanthorizons";

    public static boolean enableDespawnModule = true;

    public static int maxMobBlockLightLevel = 0;

    public static int maxMobSkyLightLevel = 7;

    public static boolean enableProjectETweaks = true;

    public static boolean enableThaumicAdditionsTweaks = true;

    public static boolean enableNEITweaks = true;

    public static boolean enableMCPatcherTweaks = true;

    public static boolean enableDistantHorizonsDimensionFilter = true;

    public static int[] distantHorizonsDimensionIds = new int[0];

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
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable module that lets mobs holding items/armor despawn");

        maxMobBlockLightLevel = configuration.getInt(
            "MaxMobSpawnLightLevel",
            Configuration.CATEGORY_GENERAL,
            0,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using block light");

        maxMobSkyLightLevel = configuration.getInt(
            "MaxMobSkyLightLevel",
            Configuration.CATEGORY_GENERAL,
            7,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using sky light");

        enableProjectETweaks = configuration.getBoolean(
            "EnableProjectETweaks",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable fixes and tweaks for ProjectE");

        enableThaumicAdditionsTweaks = configuration.getBoolean(
            "EnableThaumicAdditionsTweaks",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable fixes and tweaks for Thaumic Additions");

        enableNEITweaks = configuration.getBoolean(
            "EnableNEITweaks",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable fixes and tweaks for Not Enough Items");

        enableMCPatcherTweaks = configuration.getBoolean(
            "EnableMCPatcherTweaks",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable fixes and tweaks for MCPatcher");

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

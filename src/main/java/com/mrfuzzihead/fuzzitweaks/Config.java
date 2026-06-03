package com.mrfuzzihead.fuzzitweaks;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean enableDespawnModule = true;

    public static int maxMobBlockLightLevel = 0;

    public static int maxMobSkyLightLevel = 7;

    public static boolean enableProjectETweaks = true;

    public static boolean enableThaumicAdditionsTweaks = true;

    public static boolean enableNEITweaks = true;

    public static boolean enableMCPatcherTweaks = true;

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
            0,
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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}

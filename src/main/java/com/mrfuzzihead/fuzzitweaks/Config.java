package com.mrfuzzihead.fuzzitweaks;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean enableDespawnModule = true;

    public static int maxMobLightLevel = 0;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        enableDespawnModule = configuration.getBoolean(
            "EnableDespawnModule",
            Configuration.CATEGORY_GENERAL,
            true,
            "Enable module that lets mobs holding items/armor despawn");

        maxMobLightLevel = configuration.getInt(
            "MaxMobSpawnLightLevel",
            Configuration.CATEGORY_GENERAL,
            0,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15)");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}

package com.mrfuzzihead.fuzzitweaks;

import com.mrfuzzihead.fuzzitweaks.common.AIGoalStripper;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        // Only registers a listener when one of the look-goal removals is enabled.
        AIGoalStripper.register();
    }
}

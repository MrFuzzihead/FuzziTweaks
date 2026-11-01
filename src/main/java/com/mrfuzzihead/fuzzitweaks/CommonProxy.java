package com.mrfuzzihead.fuzzitweaks;

import com.mrfuzzihead.fuzzitweaks.common.AIGoalStripper;
import com.mrfuzzihead.fuzzitweaks.common.DoubleDoors;
import com.mrfuzzihead.fuzzitweaks.common.util.RailcraftPresence;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        RailcraftPresence.init();
        // Only registers a listener when one of the look-goal removals is enabled.
        AIGoalStripper.register();
        // Registers the door handler; the handler also reverts client predictions when the option is off.
        DoubleDoors.register();
    }
}

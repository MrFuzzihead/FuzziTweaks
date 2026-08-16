package com.mrfuzzihead.fuzzitweaks;

import com.mrfuzzihead.fuzzitweaks.common.GalacticraftRecipes;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = FuzziTweaks.MODID,
    version = Tags.VERSION,
    name = FuzziTweaks.MODNAME,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    dependencies = FuzziTweaks.DEPENDENCIES)
public class FuzziTweaks {

    public static final String MODID = "fuzzitweaks";
    public static final String MODNAME = "FuzziTweaks";
    public static final String DEPENDENCIES = "after:ProjectE;after:thaumicadditions;after:NotEnoughItems;after:mcpatcher;after:GalacticraftCore;after:distanthorizons";

    @SidedProxy(
        clientSide = "com.mrfuzzihead.fuzzitweaks.ClientProxy",
        serverSide = "com.mrfuzzihead.fuzzitweaks.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Restore Galacticraft recipes the GTNH fork gated behind its coremod/GregTech.
        // Guarded so the GC-referencing class is only touched when GC is present.
        if (Loader.isModLoaded("GalacticraftCore")) {
            GalacticraftRecipes.register();
        }
    }
}

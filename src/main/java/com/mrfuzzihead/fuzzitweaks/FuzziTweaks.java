package com.mrfuzzihead.fuzzitweaks;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
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
}

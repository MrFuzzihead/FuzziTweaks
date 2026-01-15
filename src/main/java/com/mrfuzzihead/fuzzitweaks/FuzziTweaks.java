package com.mrfuzzihead.fuzzitweaks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = FuzziTweaks.MODID, version = Tags.VERSION, name = "FuzziTweaks", acceptedMinecraftVersions = "[1.7.10]")
public class FuzziTweaks {

    public static final String MODID = "fuzzitweaks";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.mrfuzzihead.fuzzitweaks.ClientProxy",
        serverSide = "com.mrfuzzihead.fuzzitweaks.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}

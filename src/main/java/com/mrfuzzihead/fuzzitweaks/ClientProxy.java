package com.mrfuzzihead.fuzzitweaks;

import com.mrfuzzihead.fuzzitweaks.client.ThreadedScreenshot;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        if (Config.enableBackgroundScreenshot) {
            ThreadedScreenshot.init();
        }
    }
}

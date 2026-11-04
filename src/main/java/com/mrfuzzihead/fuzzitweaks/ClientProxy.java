package com.mrfuzzihead.fuzzitweaks;

import net.minecraftforge.client.ClientCommandHandler;

import com.mrfuzzihead.fuzzitweaks.client.ThreadedScreenshot;
import com.mrfuzzihead.fuzzitweaks.client.command.CommandWind;
import com.mrfuzzihead.fuzzitweaks.common.util.ServerModPresence;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        if (Config.enableDoubleDoors) {
            ServerModPresence.register();
        }
        if (Config.enableBackgroundScreenshot) {
            ThreadedScreenshot.init();
        }
        // Registered even when the wind is disabled, so the command can explain why it is not moving anything.
        ClientCommandHandler.instance.registerCommand(new CommandWind());
    }
}

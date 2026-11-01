package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.Map;

import com.mrfuzzihead.fuzzitweaks.FuzziTweaks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * Remembers whether the server we are connected to also runs FuzziTweaks.
 *
 * <p>
 * The double-door handler predicts the second leaf on the client so both leaves open in the same frame. That
 * prediction is only safe when the server actually performs the second activation; otherwise the predicted leaf
 * would stay open on a server that has no FuzziTweaks. The FML handshake's {@code ModList} contains the mod IDs of
 * the remote side, so the client can tell whether the server has the mod before predicting anything. The flag is
 * reset on disconnect, so a later visit to a vanilla server does not reuse the previous server's answer.
 */
public final class ServerModPresence {

    private static final ServerModPresence INSTANCE = new ServerModPresence();

    /** Written on the network thread during the handshake and read on the client thread. */
    private static volatile boolean serverHasFuzziTweaks;

    private ServerModPresence() {}

    /**
     * Registers the disconnect listener. Called from the client proxy only.
     */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    /**
     * Records the remote mod list. The FML handshake calls {@code ModList#modList()} on both sides; on an integrated
     * server that also records the client's list, which is the same installation and therefore gives the same answer.
     */
    public static void onHandshakeModList(Map<String, String> modList) {
        serverHasFuzziTweaks = modList != null && modList.containsKey(FuzziTweaks.MODID);
    }

    public static boolean hasFuzziTweaks() {
        return serverHasFuzziTweaks;
    }

    static void clear() {
        serverHasFuzziTweaks = false;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clear();
    }
}

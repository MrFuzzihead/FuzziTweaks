package com.mrfuzzihead.fuzzitweaks.mixins.early;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.common.util.ServerModPresence;

import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;

/**
 * Captures the remote side's FML handshake mod list for the double-door client prediction.
 *
 * <p>
 * {@code ModList#modList()} is called by {@code FMLNetworkHandler.checkModList} during the real handshake. The
 * multiplayer server-list ping uses a plain {@code Map} instead, so browsing the server list cannot set the flag.
 */
@Mixin(FMLHandshakeMessage.ModList.class)
public abstract class FMLHandshakeModListMixin {

    @Inject(method = "modList", at = @At("RETURN"), remap = false, require = 1)
    private void fuzziTweaks$captureRemoteMods(CallbackInfoReturnable<Map<String, String>> cir) {
        ServerModPresence.onHandshakeModList(cir.getReturnValue());
    }
}

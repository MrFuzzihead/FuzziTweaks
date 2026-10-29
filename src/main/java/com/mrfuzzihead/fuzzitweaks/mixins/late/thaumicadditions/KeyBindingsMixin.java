package com.mrfuzzihead.fuzzitweaks.mixins.late.thaumicadditions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.pengu.thaumcraft.additions.proxy.KeyBindings;

/**
 * Suppresses Thaumic Additions' login-time version/update chat messages.
 *
 * <p>
 * {@code KeyBindings#onTick} only checks the version gathered by {@code TA.gatherVersionInfo()}
 * and prints the outdated / what's-new / up-to-date / no-connection message once per login. It
 * has nothing to do with the HUD or key bindings despite the class name. The handler is gated on
 * its own option so the HUD toggle remains independent.
 */
@Mixin(KeyBindings.class)
public abstract class KeyBindingsMixin {

    @Inject(method = "onTick", at = @At("HEAD"), remap = false, cancellable = true)
    private void fuzziTweaks$suppressVersionNotifier(CallbackInfo ci) {
        if (Config.disableThaumicAdditionsVersionNotifier) {
            ci.cancel();
        }
    }
}

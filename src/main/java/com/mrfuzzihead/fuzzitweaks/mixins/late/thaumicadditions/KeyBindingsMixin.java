package com.mrfuzzihead.fuzzitweaks.mixins.late.thaumicadditions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pengu.thaumcraft.additions.proxy.KeyBindings;

@Mixin(KeyBindings.class)
public abstract class KeyBindingsMixin {

    @Inject(method = "onTick", at = @At("HEAD"), remap = false, cancellable = true)
    private void onTick(CallbackInfo ci) {
        ci.cancel();
    }
}

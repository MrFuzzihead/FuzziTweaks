package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.client.wind.WindRuntime;

/**
 * Applies the simulated wind to every particle the effect renderer ticks.
 *
 * <p>
 * The wrap is on {@code EffectRenderer.updateEffects()}'s call to {@code EntityFX.onUpdate()} rather than on
 * {@code EntityFX.onUpdate()} itself: vanilla and modded particles freely override {@code onUpdate}, so mixing
 * into the base class would miss exactly the long-lived campfire and smoker particles the wind is meant for.
 * The call site covers all of them.
 */
@Mixin(EffectRenderer.class)
public abstract class EffectRendererMixin {

    @WrapOperation(
        method = "updateEffects",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;onUpdate()V"),
        require = 1)
    private void fuzziTweaks$applyWind(EntityFX particle, Operation<Void> original) {
        WindRuntime.applyTo(particle);
        original.call(particle);
    }
}

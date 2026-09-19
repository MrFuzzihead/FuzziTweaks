package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Fixes the vanilla 1.7.10 melee attack rate bug in place, without replacing the AI task.
 *
 * <p>
 * Vanilla {@code updateTask} clamps its own counter to {@code 0..20} and then tests
 * {@code attackTick <= 20}, so the check is always true: in range, a mob calls {@code swingItem()} and
 * {@code attackEntityAsMob()} every single tick. Damage still lands roughly every 10 ticks thanks to
 * {@code hurtResistantTime}, but that is twice as fast as the intended 20 tick (1 second) cooldown
 * (1.8+ vanilla tests {@code attackTick <= 0}), and the swing animation is spammed every tick.
 *
 * <p>
 * This is the fix AI Improvements (MIT, BuiltBrokenModding/DarkCow) shipped as
 * {@code EntityAIAttackOnCollideOverride}; here it lives in the vanilla class itself, so it applies to
 * every user of {@code EntityAIAttackOnCollide} (vanilla and modded) instead of only to entity types
 * a player opted in by hand. The counts stay per task instance, so each AI entry on a mob rates limits
 * independently, and vanilla's own bookkeeping is left untouched.
 */
@Mixin(EntityAIAttackOnCollide.class)
public abstract class EntityAIAttackOnCollideMixin {

    /** Ticks left before this task may attack again. */
    @Unique
    private int fuzziTweaks$attackCooldown;

    @Inject(method = "updateTask", at = @At("HEAD"))
    private void fuzziTweaks$tickCooldown(CallbackInfo ci) {
        if (this.fuzziTweaks$attackCooldown > 0) {
            --this.fuzziTweaks$attackCooldown;
        }
    }

    @Redirect(
        method = "updateTask",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityCreature;swingItem()V"))
    private void fuzziTweaks$maybeSwing(EntityCreature instance) {
        if (this.fuzziTweaks$attackCooldown <= 0) {
            instance.swingItem();
        }
    }

    @Redirect(
        method = "updateTask",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityCreature;attackEntityAsMob(Lnet/minecraft/entity/Entity;)Z"))
    private boolean fuzziTweaks$maybeAttack(EntityCreature instance, Entity target) {
        if (this.fuzziTweaks$attackCooldown > 0) {
            return false;
        }

        this.fuzziTweaks$attackCooldown = Config.meleeAttackCooldownTicks;
        return instance.attackEntityAsMob(target);
    }
}

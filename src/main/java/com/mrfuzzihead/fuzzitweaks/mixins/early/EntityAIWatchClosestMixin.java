package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIWatchClosest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.common.util.PlayerProximity;

/**
 * Stops {@code EntityAIWatchClosest} from starting while no player is close enough to see it.
 *
 * <p>
 * The goal only turns the mob's head towards the closest player, so with nobody around there is nothing to
 * observe - and the head relaxes back onto the body exactly like it does when the goal's own
 * {@code lookTime} runs out. This also covers {@code EntityAIWatchClosest2} (villagers), which inherits
 * this method, and skips the full AABB entity scan those goals cost when they watch a non-player class.
 *
 * <p>
 * Only {@code shouldExecute} is gated, which is enough: an already running goal keeps tracking until its
 * own timer expires, and only ever for a player who is by definition already in range.
 */
@Mixin(EntityAIWatchClosest.class)
public abstract class EntityAIWatchClosestMixin {

    @Shadow
    private EntityLiving theWatcher;

    @Inject(method = "shouldExecute", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$requirePlayerInRange(CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerProximity.isAnyPlayerWithinRange(this.theWatcher)) {
            cir.setReturnValue(false);
        }
    }
}

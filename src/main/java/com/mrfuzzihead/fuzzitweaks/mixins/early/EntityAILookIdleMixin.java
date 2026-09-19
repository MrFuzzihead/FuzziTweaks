package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAILookIdle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.common.util.PlayerProximity;

/**
 * Stops {@code EntityAILookIdle} from starting while no player is close enough to see it.
 *
 * <p>
 * The goal only drifts the mob's head towards a random point near itself, so it is entirely visual. See
 * {@link EntityAIWatchClosestMixin} for why gating only {@code shouldExecute} is sufficient.
 */
@Mixin(EntityAILookIdle.class)
public abstract class EntityAILookIdleMixin {

    @Shadow
    private EntityLiving idleEntity;

    @Inject(method = "shouldExecute", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$requirePlayerInRange(CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerProximity.isAnyPlayerWithinRange(this.idleEntity)) {
            cir.setReturnValue(false);
        }
    }
}

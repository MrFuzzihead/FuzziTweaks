package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.common.util.FastTrig;

/**
 * Replaces the two {@link Math#atan2(double, double)} calls in {@code EntityLiving.faceEntity} with the
 * table based approximation.
 *
 * <p>
 * This is the second hottest look-math site after {@code EntityLookHelper}: it is called every tick for
 * every mob that runs the legacy (non-goal) AI while it has a target - {@code EntityCreature} and
 * {@code EntityLiving} call it from their {@code updateEntityActionState}, and {@code EntityEnderman} and
 * {@code EntitySlime} call it directly - which covers spiders, cave spiders, silverfish, endermen,
 * slimes, magma cubes and zombie pigmen.
 *
 * <p>
 * The results only ever feed {@code updateRotation} for yaw/pitch, so the sub-degree difference is
 * invisible.
 */
@Mixin(EntityLiving.class)
public abstract class EntityLivingFaceEntityMixin {

    @Redirect(method = "faceEntity", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"))
    private double fuzziTweaks$fastAtan2(double y, double x) {
        return FastTrig.atan2(y, x);
    }
}

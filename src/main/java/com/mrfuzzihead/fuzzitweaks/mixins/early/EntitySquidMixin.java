package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.passive.EntitySquid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.common.util.FastTrig;

/**
 * Replaces the two {@link Math#atan2(double, double)} calls in {@code EntitySquid.onLivingUpdate} with the
 * table based approximation.
 *
 * <p>
 * Squids run no AI tasks at all - all of their movement lives in custom code - and these two calls happen
 * every tick for every squid in water (they set {@code renderYawOffset}/{@code squidPitch}, which only the
 * squid renderer reads), which makes them the third hot look-math site after {@code EntityLookHelper} and
 * {@code EntityLiving.faceEntity}.
 */
@Mixin(EntitySquid.class)
public abstract class EntitySquidMixin {

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"))
    private double fuzziTweaks$fastAtan2(double y, double x) {
        return FastTrig.atan2(y, x);
    }
}

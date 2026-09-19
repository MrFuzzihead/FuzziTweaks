package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.ai.EntityLookHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.common.util.FastTrig;

/**
 * Replaces the two {@link Math#atan2(double, double)} calls in
 * {@code EntityLookHelper.onUpdateLook()} with a table based approximation.
 *
 * <p>
 * Ported from AI Improvements (MIT, BuiltBrokenModding/DarkCow), which swapped the whole
 * {@code EntityLookHelper} instance out for a subclass. Redirecting the call sites achieves the same
 * with no access transformer, no {@code EntityJoinWorldEvent} listener and no look-helper state copy -
 * and it keeps the old mod's mod-compat semantics: a mod that subclasses {@code EntityLookHelper} and
 * overrides {@code onUpdateLook} never reaches this redirect, exactly like the old
 * {@code getLookHelper().getClass() == EntityLookHelper.class} guard did.
 */
@Mixin(EntityLookHelper.class)
public abstract class EntityLookHelperMixin {

    @Redirect(method = "onUpdateLook", at = @At(value = "INVOKE", target = "Ljava/lang/Math;atan2(DD)D"))
    private double fuzziTweaks$fastAtan2(double y, double x) {
        return FastTrig.atan2(y, x);
    }
}

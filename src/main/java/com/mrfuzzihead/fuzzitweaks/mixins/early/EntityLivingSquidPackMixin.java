package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntitySquid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Squid pack-size cap, ported from the Squidless mod.
 *
 * <p>
 * Squidless listened for {@code LivingPackSizeEvent} and set {@code event.maxPackSize = 2}, but
 * {@code ForgeEventFactory.getMaxSpawnPackSize} only reads that value when a handler also calls
 * {@code setResult(ALLOW)}. The result stayed {@code DEFAULT}, so the value was discarded and vanilla's
 * {@code EntityLiving} value of 4 was always used - the pack cap never actually worked. This mixin returns the
 * configured cap directly from the declaring method instead, so the intended behaviour now takes effect.
 *
 * <p>
 * {@code EntitySquid} does not override {@code getMaxSpawnedInChunk}, so {@code EntityLiving} is the declaring
 * class. As in Squidless, only the exact {@code EntitySquid} class is affected.
 */
@Mixin(EntityLiving.class)
public abstract class EntityLivingSquidPackMixin {

    @Inject(method = "getMaxSpawnedInChunk", at = @At("HEAD"), cancellable = true, require = 1)
    private void fuzziTweaks$capSquidPackSize(CallbackInfoReturnable<Integer> cir) {
        if (Config.squidMaxPackSize > 0 && ((Object) this).getClass() == EntitySquid.class) {
            cir.setReturnValue(Config.squidMaxPackSize);
        }
    }
}

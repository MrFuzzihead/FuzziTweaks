package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a lead knot from killing itself against an unloaded chunk.
 *
 * <p>
 * {@code EntityLeashKnot}s are never written to chunk NBT (they return false from
 * {@code writeToNBTOptional}), so the one backing a mob's lead is rebuilt from the mob's own NBT. Vanilla
 * builds it the moment the mob ticks, which can be before the chunk holding the fence is resident, and
 * {@code EntityHanging#onUpdate} then re-validates the knot against {@code onValidSurface} - a plain
 * {@code getBlock(...).getRenderType() == 11} check, which reports air for a chunk that is not loaded. The
 * knot kills itself, and the mob's next tick sees a dead anchor and drops the lead.
 *
 * <p>
 * Reporting a valid surface while the support chunk is not loaded defers that check by another 100 ticks,
 * which is all it takes for the fence to be there. Once the chunk is resident the real check runs and a
 * knot whose fence really was removed still dies as it should. Only leash knots are affected;
 * {@code EntityHanging} is the shared base of paintings too, and those are never ticked from an unloaded
 * chunk, so they keep vanilla's exact behaviour.
 */
@Mixin(EntityLeashKnot.class)
public abstract class EntityLeashKnotMixin extends EntityHanging {

    public EntityLeashKnotMixin(World world) {
        super(world);
    }

    @Inject(method = "onValidSurface", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$ignoreUnloadedSupport(CallbackInfoReturnable<Boolean> cir) {
        if (!this.worldObj.blockExists(this.field_146063_b, this.field_146064_c, this.field_146062_d)) {
            cir.setReturnValue(true);
        }
    }
}

package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzzitweaks.common.util.LeashLoadGrace;

/**
 * Keeps a lead tied to a fence post from snapping off while the mob's chunk is still loading.
 *
 * <p>
 * Vanilla 1.7.10 resolves a saved lead lazily, on the mob's first tick after its chunk is read: {@code
 * readEntityFromNBT} stashes the {@code Leash} compound in a private field, {@code EntityLiving#recreateLeash}
 * turns it into an anchor entity during {@code updateLeashedState}, and that same method breaks the lead - as a
 * dropped item - as soon as no usable anchor came out of it. Whichever way the load order falls out, a mob
 * whose anchor is not resolvable on that single tick is unrecoverable, and vanilla cannot tell "not loaded
 * yet" apart from "fence is gone".
 *
 * <p>
 * Both halves of the round trip are covered here: the stash is remembered beyond the one tick vanilla gives
 * it, the anchor is looked up again on every tick of a short grace window, and {@code clearLeashed} - which
 * every automatic break funnels through, including the out-of-range break in {@code EntityCreature} - is held
 * off while that window lasts. The anchor is also written back out on save, so the unrecoverable "flagged as
 * leashed but with no anchor" NBT never reaches disk. See {@link LeashLoadGrace}.
 */
@Mixin(EntityLiving.class)
public abstract class EntityLivingLeashMixin extends EntityLivingBase {

    /** The anchor stashed by {@code readEntityFromNBT}, which vanilla consumes and drops on the first tick. */
    @Shadow
    private NBTTagCompound field_110170_bx;

    public EntityLivingLeashMixin(World world) {
        super(world);
    }

    @Inject(method = "readEntityFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    private void fuzziTweaks$beginLeashGrace(NBTTagCompound tag, CallbackInfo ci) {
        LeashLoadGrace.onEntityLoaded((EntityLiving) (Object) this, this.field_110170_bx);
    }

    @Inject(method = "updateLeashedState", at = @At("HEAD"))
    private void fuzziTweaks$tickLeashGrace(CallbackInfo ci) {
        LeashLoadGrace.tickLeash((EntityLiving) (Object) this);
    }

    @Inject(method = "clearLeashed", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$deferAnchorLoss(boolean sendPacket, boolean dropLead, CallbackInfo ci) {
        if (LeashLoadGrace.deferAnchorLoss((EntityLiving) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void fuzziTweaks$writeLeashAnchor(NBTTagCompound tag, CallbackInfo ci) {
        LeashLoadGrace.writeLeashAnchor((EntityLiving) (Object) this, tag);
    }
}

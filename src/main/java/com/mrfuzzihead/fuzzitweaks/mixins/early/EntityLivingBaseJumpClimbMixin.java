package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets a player climb a ladder by holding jump, without having to push into the ladder first.
 *
 * <p>
 * Vanilla 1.7.10 sets {@code motionY = 0.2} at the end of {@code moveEntityWithHeading} only when the entity is
 * collided horizontally and on a ladder. LegacyFixes made that condition true while jumping by modifying a field read
 * at a particular ordinal, which is brittle when another coremod also rewrites movement. This version leaves the
 * vanilla condition alone and repeats the same {@code motionY} assignment at the end of the method, so it has the
 * same timing as the vanilla climb (both run after {@code moveEntity}) without an instruction-ordinal dependency.
 *
 * <p>
 * Client-only and limited to players. Water/lava movement and creative flight are left alone, so swimming or flying
 * near a ladder does not get an extra climb impulse. The slide-climbing variant is intentionally not included; the
 * two movement tweaks conflict.
 */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseJumpClimbMixin {

    @Shadow
    protected boolean isJumping;

    /**
     * The velocity vanilla's ladder climb leaves for the next tick. {@code moveEntityWithHeading} assigns 0.2 before
     * its final gravity ({@code -0.08}) and drag ({@code *0.98}) steps, so the value that actually moves the entity is
     * 0.1176. Setting that value at TAIL reproduces the same climb speed after those steps and avoids injecting at a
     * field-read ordinal.
     */
    private static final double LADDER_CLIMB_MOTION = (0.2D - 0.08D) * 0.9800000190734863D;

    @Inject(method = "moveEntityWithHeading", at = @At("TAIL"), require = 1)
    private void fuzziTweaks$jumpClimbLadder(float strafe, float forward, CallbackInfo ci) {
        final EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (!(self instanceof EntityPlayer)) {
            return;
        }

        final EntityPlayer player = (EntityPlayer) self;
        if (!this.isJumping || !self.isOnLadder()
            || self.isInWater()
            || self.handleLavaMovement()
            || player.capabilities.isFlying) {
            return;
        }

        self.motionY = LADDER_CLIMB_MOTION;
    }
}

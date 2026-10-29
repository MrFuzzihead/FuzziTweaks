package com.mrfuzzihead.fuzzitweaks.mixins.early;

import java.util.Random;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.Config;

@Mixin(EntityMob.class)
public class EntityMobMixin extends EntityCreature implements IMob {

    public EntityMobMixin(World p_i1602_1_) {
        super(p_i1602_1_);
        this.experienceValue = 5;
    }

    /**
     * Rejects excess block light after vanilla's raw sky-light gate but before
     * the thundering/brightness calculation, so failing the block check skips
     * the rest of the method.
     */
    @Inject(
        method = "isValidLightLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockLightValue(III)I",
            ordinal = 0,
            shift = At.Shift.BEFORE),
        cancellable = true,
        require = 1)
    protected void fuzziTweaks$rejectExcessBlockLight(CallbackInfoReturnable<Boolean> cir) {
        final int x = MathHelper.floor_double(this.posX);
        final int y = MathHelper.floor_double(this.boundingBox.minY);
        final int z = MathHelper.floor_double(this.posZ);

        if (this.worldObj.getSavedLightValue(EnumSkyBlock.Block, x, y, z) > Config.maxMobBlockLightLevel) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Vanilla has already applied its thundering adjustment to the brightness
     * used by this final comparison; only the random threshold changes.
     */
    @WrapOperation(
        method = "isValidLightLevel",
        at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 1),
        require = 1)
    private int fuzziTweaks$replaceFinalLightThreshold(final Random random, final int bound,
        final Operation<Integer> original) {
        return original.call(random, Config.maxMobSkyLightLevel + 1);
    }
}

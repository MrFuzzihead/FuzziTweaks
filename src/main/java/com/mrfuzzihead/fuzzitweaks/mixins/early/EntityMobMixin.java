package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;

@Mixin(EntityMob.class)
public class EntityMobMixin extends EntityCreature implements IMob {

    @Unique
    protected final int fuzziTweaks$maxMobLightLevel;

    public EntityMobMixin(World p_i1602_1_) {
        super(p_i1602_1_);
        this.experienceValue = 5;
        this.fuzziTweaks$maxMobLightLevel = Config.maxMobLightLevel;
    }

    @Inject(method = "isValidLightLevel", at = @At("TAIL"), cancellable = true)
    protected void fuzziTweaks$redirectMaxLightLevelCheck(CallbackInfoReturnable<Boolean> cir) {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.boundingBox.minY);
        int z = MathHelper.floor_double(this.posZ);
        int lightInclSky = this.worldObj.getBlockLightValue(x, y, z);
        int lightExclSky = this.worldObj.getSavedLightValue(EnumSkyBlock.Block, x, y, z);

        if (lightInclSky <= this.rand.nextInt(8) && lightExclSky <= this.fuzziTweaks$maxMobLightLevel) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }
}

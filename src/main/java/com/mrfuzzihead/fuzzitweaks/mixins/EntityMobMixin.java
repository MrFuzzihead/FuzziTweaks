package com.mrfuzzihead.fuzzitweaks.mixins;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

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

    /**
     * @author MrFuzzihead
     * @reason Changing Minecraft hostile mob spawning in 1.7.10 to set valid light level
     * as configurable value based on {@link #fuzziTweaks$maxMobLightLevel}
     */
    @Overwrite
    protected boolean isValidLightLevel() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY);
        int z = MathHelper.floor_double(this.posZ);

        if (this.worldObj.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) > this.rand.nextInt(32)) {
            return false;
        } else {
            int lightInclSky = this.worldObj.getBlockLightValue(x, y, z);
            int lightExclSky = this.worldObj.getSavedLightValue(EnumSkyBlock.Block, x, y, z);

            if (this.worldObj.isThundering()) {
                int tempSkyLightSubtracted = this.worldObj.skylightSubtracted;
                this.worldObj.skylightSubtracted = 10;
                lightInclSky = this.worldObj.getBlockLightValue(x, y, z);
                this.worldObj.skylightSubtracted = tempSkyLightSubtracted;
            }

            return lightInclSky <= this.rand.nextInt(8) && lightExclSky <= fuzziTweaks$maxMobLightLevel;
        }
    }
}

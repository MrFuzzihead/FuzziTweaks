package com.mrfuzzihead.fuzzitweaks.mixins;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.mrfuzzihead.fuzzitweaks.Config;

@Mixin(EntityMob.class)
public class EntityMobMixin extends EntityCreature implements IMob {

    protected int maxMobLightLevel;

    public EntityMobMixin(World p_i1602_1_) {
        super(p_i1602_1_);
        this.experienceValue = 5;
        this.maxMobLightLevel = Config.maxMobLightLevel;
    }

    /**
     * @author MrFuzzihead
     * @reason Changing Minecraft hostile mob spawning in 1.7.10 to set valid light level as 0 only
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

            /**
             * If the world is thundering, mobs can spawn wherever the block's light level (excluding skylight) is lower
             * than the maximum spawn light level.
             * If the world is not thundering, mobs can begin spawning once the skylight reaches 7 and below. We will
             * instead then take the block's light level (excluding skylight).
             */
            if (this.worldObj.isThundering() || lightInclSky < 8) {
                return lightExclSky <= this.rand.nextInt(maxMobLightLevel + 1);
            }

            return lightInclSky <= this.rand.nextInt(maxMobLightLevel + 1);
        }
    }
}

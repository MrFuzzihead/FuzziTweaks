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

    public EntityMobMixin(World p_i1602_1_) {
        super(p_i1602_1_);
        this.experienceValue = 5;
    }

    /**
     * @author MrFuzzihead
     * @reason Changing Minecraft hostile mob spawning in 1.7.10 to set valid light level as 0 only
     */
    @Overwrite
    protected boolean isValidLightLevel() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.boundingBox.minY);
        int z = MathHelper.floor_double(this.posZ);
        int maxMobLightLevel = Config.maxMobLightLevel;

        if (this.worldObj.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) > this.rand.nextInt(32)) {
            return false;
        } else {
            int blockLightInclSky = this.worldObj.getBlockLightValue(x, y, z);
            int blockLightExclSky = this.worldObj.getSavedLightValue(EnumSkyBlock.Block, x, y, z);

            if (this.worldObj.isThundering() || (blockLightInclSky < 8 && blockLightInclSky > blockLightExclSky)) {
                return blockLightExclSky <= this.rand.nextInt(maxMobLightLevel + 1);
            }

            return blockLightInclSky <= this.rand.nextInt(maxMobLightLevel + 1);
        }
    }
}

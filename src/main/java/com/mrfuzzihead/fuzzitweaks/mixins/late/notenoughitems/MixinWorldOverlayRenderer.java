package com.mrfuzzihead.fuzzitweaks.mixins.late.notenoughitems;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mrfuzzihead.fuzzitweaks.Config;

import codechicken.nei.WorldOverlayRenderer;

@Mixin(WorldOverlayRenderer.class)
public class MixinWorldOverlayRenderer {

    @Final
    @Shadow(remap = false)
    private static Entity dummyEntity;
    @Final
    @Shadow(remap = false)
    private static AxisAlignedBB aabb;

    /**
     * @author FuzziTweaks
     * @reason Make NEI's mob-spawn overlay use the same block-light and sky-light thresholds as the FuzziTweaks
     *         spawn rules. The vanilla method hardcodes a block-light limit of 8 and an effective sky-light
     *         limit of 8; the collision, liquid and return-value logic is otherwise preserved.
     */
    @Overwrite(remap = false)
    private static byte getSpawnMode(Chunk chunk, int x, int y, int z) {
        // Above world height, no spawns here
        if (y >= chunk.worldObj.getHeight()) {
            return 0;
        }

        // Block light check (supersedes skylight value check)
        if (chunk.getSavedLightValue(EnumSkyBlock.Block, x & 15, y, z & 15) > Config.maxMobBlockLightLevel
            || !SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, chunk.worldObj, x, y, z)) {
            return 0;
        }

        aabb.minX = x + 0.2;
        aabb.maxX = x + 0.8;
        aabb.minY = y + 0.01;
        aabb.maxY = y + 1.8;
        aabb.minZ = z + 0.2;
        aabb.maxZ = z + 0.8;
        if (!chunk.worldObj.getCollidingBoundingBoxes(dummyEntity, aabb)
            .isEmpty() || chunk.worldObj.isAnyLiquid(aabb)) {
            return 0;
        }

        // Sky light check
        return (byte) (chunk.getSavedLightValue(EnumSkyBlock.Sky, x & 15, y, z & 15) >= (Config.maxMobSkyLightLevel + 1)
            ? 1
            : 2);
    }
}

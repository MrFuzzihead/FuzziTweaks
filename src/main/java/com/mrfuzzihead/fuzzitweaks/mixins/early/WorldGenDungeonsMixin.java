package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDungeons;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Careful Cast Corrector, {@code WorldGenDungeons} half.
 *
 * <p>
 * Dungeon generation looks the freshly placed chest and mob spawner up with hard casts to
 * {@code TileEntityChest} / {@code TileEntityMobSpawner}. A mod that provides its own tile entity for either
 * block (or a block that was already present at the target coordinates) makes those casts throw and aborts
 * world generation. CCC replaced both casts with {@code instanceof} checks, routing foreign tile entities
 * through its two extension hooks; with no hook registered the generation step is simply skipped. This port
 * reproduces that: the lookups are redirected to return {@code null} for a foreign tile entity, so the
 * vanilla null guards skip the loot fill / spawner setup instead of crashing.
 *
 * <p>
 * The CCC extension API is intentionally not ported - nothing in the pack registers a handler. Vanilla's
 * {@code "Failed to fetch mob spawner entity"} message is left as-is for visibility.
 */
@Mixin(WorldGenDungeons.class)
public abstract class WorldGenDungeonsMixin {

    @Redirect(
        method = "generate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getTileEntity(III)Lnet/minecraft/tileentity/TileEntity;",
            ordinal = 0),
        require = 1)
    private TileEntity fuzziTweaks$safeDungeonChestLookup(World world, int x, int y, int z) {
        final TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityChest ? tileEntity : null;
    }

    @Redirect(
        method = "generate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getTileEntity(III)Lnet/minecraft/tileentity/TileEntity;",
            ordinal = 1),
        require = 1)
    private TileEntity fuzziTweaks$safeDungeonSpawnerLookup(World world, int x, int y, int z) {
        final TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityMobSpawner ? tileEntity : null;
    }
}

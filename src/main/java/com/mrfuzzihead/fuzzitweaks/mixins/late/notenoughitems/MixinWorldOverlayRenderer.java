package com.mrfuzzihead.fuzzitweaks.mixins.late.notenoughitems;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.Config;

import codechicken.nei.WorldOverlayRenderer;

@Mixin(WorldOverlayRenderer.class)
public class MixinWorldOverlayRenderer {

    /**
     * Redirects the block light check in getSpawnMode.
     * Returns 8 (triggering "no spawn") when block light exceeds the configured max,
     * otherwise returns 0 so the vanilla >= 8 check passes through as spawnable.
     */
    @Redirect(
        method = "getSpawnMode",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getSavedLightValue(Lnet/minecraft/world/EnumSkyBlock;III)I",
            ordinal = 0))
    private static int fuzziTweaks$redirectBlockLightCheck(Chunk chunk, EnumSkyBlock type, int x, int y, int z) {
        int actualLight = chunk.getSavedLightValue(type, x, y, z);
        return actualLight > Config.maxMobBlockLightLevel ? 8 : 0;
    }

    /**
     * Redirects the skylight check in getSpawnMode.
     * Returns 8 (yellow X — night-only spawn) when skylight exceeds the configured max,
     * otherwise returns 0 (red X — always spawnable).
     */
    @Redirect(
        method = "getSpawnMode",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getSavedLightValue(Lnet/minecraft/world/EnumSkyBlock;III)I",
            ordinal = 1))
    private static int fuzziTweaks$redirectSkyLightCheck(Chunk chunk, EnumSkyBlock type, int x, int y, int z) {
        int actualLight = chunk.getSavedLightValue(type, x, y, z);
        return actualLight > Config.maxMobSkyLightLevel ? 8 : 0;
    }
}

package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Squid spawn denial, ported from the Squidless mod.
 *
 * <p>
 * Squidless registered a {@code LivingSpawnEvent.CheckSpawn} handler at {@code HIGHEST} priority that denied the
 * spawn unless a player was within 64 blocks (a zero-size AABB at the spawn position expanded by 64 on every
 * axis). {@code SpawnerAnimals} fires that check for every candidate and only calls {@code getCanSpawnHere} when
 * no handler forced a result, so the closest vanilla equivalent is the spawn-validity test itself.
 *
 * <p>
 * {@code EntitySquid} overrides {@code getCanSpawnHere}, but it only adds a height window before delegating to
 * {@code super} ({@code EntityWaterMob.getCanSpawnHere}), which is why the injection lives on the declaring
 * class: a false returned here propagates up through the squid's own check. The exact-class test keeps modded
 * water mobs and modded squid subclasses on vanilla behaviour, matching Squidless' own {@code getClass() ==
 * EntitySquid.class} guard.
 */
@Mixin(EntityWaterMob.class)
public abstract class EntityWaterMobSpawnMixin extends EntityCreature {

    public EntityWaterMobSpawnMixin(World world) {
        super(world);
    }

    @Inject(method = "getCanSpawnHere", at = @At("HEAD"), cancellable = true, require = 1)
    private void fuzziTweaks$denySquidSpawnsAwayFromPlayers(CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSquidSpawnControl) {
            return;
        }

        // Match Squidless exactly: vanilla squids only, not modded water mobs or squid subclasses.
        if (((Object) this).getClass() != EntitySquid.class) {
            return;
        }

        final int range = Config.squidSpawnPlayerRange;
        final AxisAlignedBB search = AxisAlignedBB.getBoundingBox(posX, posY, posZ, posX, posY, posZ)
            .expand(range, range, range);

        if (worldObj.getEntitiesWithinAABB(EntityPlayer.class, search)
            .isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}

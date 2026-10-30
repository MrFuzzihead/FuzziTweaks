package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLiving.class)
public abstract class EntityLivingMixin extends EntityLivingBase {

    @Shadow
    protected abstract void dropEquipment(boolean wasHitByPlayer, int lootMultiplicatorPct);

    public EntityLivingMixin(World p_i1594_1_) {
        super(p_i1594_1_);
    }

    /**
     * Vanilla sets {@code persistenceRequired = true} at the end of a successful
     * equipment pickup in {@code onLivingUpdate}. Suppress that write instead of
     * scanning equipment every tick: it covers held items and all four armor
     * slots, while leaving persistence set by name tags, NBT, or other mods alone.
     */
    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLiving;persistenceRequired:Z",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void fuzziTweaks$suppressPickupPersistence(EntityLiving entity, boolean value) {
        // Intentionally empty.
    }

    /**
     * Picked-up equipment has a drop chance of 2.0, so vanilla's
     * {@code dropEquipment(false, 0)} drops only the loot the mob picked up,
     * not its naturally spawned equipment.
     */
    @Inject(
        method = "despawnEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLiving;setDead()V"),
        require = 1)
    private void fuzziTweaks$dropPickedEquipment(CallbackInfo ci) {
        this.dropEquipment(false, 0);
    }
}

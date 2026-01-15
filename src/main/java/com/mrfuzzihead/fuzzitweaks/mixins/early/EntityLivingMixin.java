package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzzitweaks.Config;

@Mixin(EntityLiving.class)
public abstract class EntityLivingMixin extends EntityLivingBase {

    @Shadow
    private boolean persistenceRequired;

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    public abstract boolean hasCustomNameTag();

    public EntityLivingMixin(World p_i1594_1_) {
        super(p_i1594_1_);
    }

    @Inject(at = @At("TAIL"), method = "onLivingUpdate")
    private void updatePersistenceStatus(CallbackInfo ci) {
        if (Config.enableDespawnModule) {
            if (this.getHeldItem() != null && !this.hasCustomNameTag()) {
                this.persistenceRequired = false;
            }
        }
    }
}

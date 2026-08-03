package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import micdoodle8.mods.galacticraft.core.inventory.SlotSpecific;

/**
 * The Coal Generator's GUI uses a {@link SlotSpecific} whose {@code isItemValid} whitelists
 * only coal / coal blocks, so burnables like charcoal can't be clicked in even though the
 * tile's {@code isItemValidForSlot} already accepts them. If a slot whitelists coal, treat it
 * as a coal/fuel slot and also accept any furnace-burnable item. This intentionally does not
 * reference the tile type, so it can't drag in GC's energy/Mekanism interfaces at load time.
 */
@Mixin(SlotSpecific.class)
public abstract class SlotSpecificMixin {

    @Shadow(remap = false)
    public ItemStack[] validItemStacks;

    @Shadow(remap = false)
    public boolean isInverted;

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$allowBurnablesInCoalSlots(ItemStack compareStack, CallbackInfoReturnable<Boolean> cir) {
        if (!this.isInverted && compareStack != null
            && TileEntityFurnace.getItemBurnTime(compareStack) > 0
            && this.fuzziTweaks$whitelistsCoal()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean fuzziTweaks$whitelistsCoal() {
        for (ItemStack stack : this.validItemStacks) {
            if (stack != null && stack.getItem() == Items.coal) {
                return true;
            }
        }
        return false;
    }
}

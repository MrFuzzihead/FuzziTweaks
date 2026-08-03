package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import micdoodle8.mods.galacticraft.core.tile.TileEntityCoalGenerator;

/**
 * Extends the Galacticraft Coal Generator to burn any furnace-burnable item
 * (charcoal, wood, mod fuels, ...), not just coal and coal blocks.
 *
 * <p>
 * GC calibrates its fuel runtime at 1/5 of a vanilla furnace burn tick
 * (1 lump of coal = 1600 furnace ticks = 320 generator ticks). We keep that
 * calibration by scaling each burnable's runtime with the same 1/5 factor, so
 * charcoal, sticks, etc. last proportionately to their vanilla burn time.
 */
@Mixin(TileEntityCoalGenerator.class)
public abstract class TileEntityCoalGeneratorMixin {

    @Shadow(remap = false)
    private ItemStack[] containingItems;

    @Shadow(remap = false)
    public int itemCookTime;

    /**
     * Allow any item with a positive furnace burn time to be inserted as fuel.
     *
     * @author FuzziTweaks
     * @reason GC only allows coal / coal blocks; accept all burnables instead.
     */
    @Overwrite
    public boolean isItemValidForSlot(int slotID, ItemStack itemstack) {
        return itemstack != null && TileEntityFurnace.getItemBurnTime(itemstack) > 0;
    }

    /**
     * Satisfy GC's internal "is this coal?" comparison so that any burnable item is
     * treated as a valid fuel source and enters the consumption path.
     */
    @Redirect(
        method = "updateEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item fuzziTweaks$anyBurnableIsCoal(ItemStack stack) {
        if (stack != null && TileEntityFurnace.getItemBurnTime(stack) > 0) {
            return Items.coal;
        }
        return stack == null ? null : stack.getItem();
    }

    /**
     * GC sets a fixed runtime (320 / 3200 ticks) when it starts consuming a fuel. Override
     * it right after that write with the burnable's vanilla burn time divided by 5, so each
     * fuel lasts proportionately to its burn time.
     */
    @Inject(
        method = "updateEntity",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lmicdoodle8/mods/galacticraft/core/tile/TileEntityCoalGenerator;itemCookTime:I",
            shift = At.Shift.AFTER))
    private void fuzziTweaks$scaleBurnTime(CallbackInfo ci) {
        ItemStack fuel = this.containingItems.length > 0 ? this.containingItems[0] : null;
        if (fuel != null) {
            int burn = TileEntityFurnace.getItemBurnTime(fuel);
            if (burn > 0) {
                this.itemCookTime = Math.max(1, burn / 5);
            }
        }
    }
}

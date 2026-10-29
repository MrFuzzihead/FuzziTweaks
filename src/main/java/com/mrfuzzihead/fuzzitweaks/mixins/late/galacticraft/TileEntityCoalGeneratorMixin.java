package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;

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
 *
 * <p>
 * The burn-time scaling is applied from the {@code decrStackSize} call instead of
 * from the {@code itemCookTime} field writes. GC writes {@code itemCookTime}
 * three times in {@code updateEntity} (the per-tick decrement and the two fuel
 * branches); a {@code shift = AFTER} injection on the field matches all three,
 * including the decrement, which reset the timer every tick while a second fuel
 * item was still in the slot and let one item burn forever.
 */
@Mixin(TileEntityCoalGenerator.class)
public abstract class TileEntityCoalGeneratorMixin {

    @Shadow(remap = false)
    private ItemStack[] containingItems;

    @Shadow(remap = false)
    public int itemCookTime;

    /**
     * Allow any item with a positive furnace burn time to be inserted as fuel.
     * The GUI slot is handled by {@link SlotSpecificMixin}; this covers hoppers,
     * pipes and the tile's own {@code canInsertItem}.
     */
    @Inject(method = "isItemValidForSlot", at = @At("HEAD"), cancellable = true)
    private void fuzziTweaks$allowBurnableFuel(int slotID, ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
        if (Config.enableGalacticraftCoalGeneratorFuelTweak) {
            cir.setReturnValue(slotID == 0 && itemstack != null && TileEntityFurnace.getItemBurnTime(itemstack) > 0);
        }
    }

    /**
     * Satisfy GC's internal "is this coal?" comparison so that any burnable item is
     * treated as a valid fuel source and enters the consumption path.
     */
    @Redirect(
        method = "updateEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item fuzziTweaks$anyBurnableIsCoal(ItemStack stack) {
        if (Config.enableGalacticraftCoalGeneratorFuelTweak && stack != null
            && TileEntityFurnace.getItemBurnTime(stack) > 0) {
            return Items.coal;
        }
        return stack == null ? null : stack.getItem();
    }

    /**
     * GC sets a fixed runtime (320 / 3200 ticks) just before consuming the fuel.
     * Override it immediately before the {@code decrStackSize} call with the
     * burnable's vanilla burn time divided by 5, so each fuel lasts
     * proportionately to its burn time. Reading the slot before consumption also
     * means the handler does not depend on the return value of the GC method.
     */
    @Inject(
        method = "updateEntity",
        at = @At(
            value = "INVOKE",
            target = "Lmicdoodle8/mods/galacticraft/core/tile/TileEntityCoalGenerator;"
                + "decrStackSize(II)Lnet/minecraft/item/ItemStack;",
            shift = At.Shift.BEFORE))
    private void fuzziTweaks$scaleBurnTime(CallbackInfo ci) {
        if (!Config.enableGalacticraftCoalGeneratorFuelTweak || this.containingItems.length == 0) {
            return;
        }
        ItemStack fuel = this.containingItems[0];
        if (fuel != null) {
            int burn = TileEntityFurnace.getItemBurnTime(fuel);
            if (burn > 0) {
                this.itemCookTime = Math.max(1, burn / 5);
            }
        }
    }
}

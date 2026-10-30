package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
 * GC sets a fixed runtime (320 / 3200 ticks) just before consuming the fuel. The
 * {@code decrStackSize} redirect replaces that runtime with the burnable's vanilla
 * burn time divided by 5, so each fuel lasts proportionately to its burn time. It
 * also returns container items (including the empty bucket left by a lava bucket)
 * instead of deleting them.
 */
@Mixin(TileEntityCoalGenerator.class)
public abstract class TileEntityCoalGeneratorMixin extends TileEntity {

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
     * Consume one fuel item, apply its scaled burn time, and return any container item.
     *
     * <p>
     * GC writes {@code itemCookTime} three times in {@code updateEntity} (the per-tick
     * decrement and the two fuel branches). Handling the write here, at the actual fuel
     * consumption point, avoids matching the decrement and accidentally resetting the
     * timer every tick while a second fuel item is still in the slot.
     */
    @Redirect(
        method = "updateEntity",
        at = @At(
            value = "INVOKE",
            target = "Lmicdoodle8/mods/galacticraft/core/tile/TileEntityCoalGenerator;"
                + "decrStackSize(II)Lnet/minecraft/item/ItemStack;"))
    private ItemStack fuzziTweaks$consumeFuel(TileEntityCoalGenerator generator, int slot, int amount) {
        final IInventory inventory = (IInventory) this;
        ItemStack fuel = inventory.getStackInSlot(slot);
        int burn = 0;
        ItemStack container = null;

        if (Config.enableGalacticraftCoalGeneratorFuelTweak && fuel != null) {
            burn = TileEntityFurnace.getItemBurnTime(fuel);
            if (burn > 0) {
                container = fuel.getItem()
                    .getContainerItem(fuel);
                // 1.7.10 has no container-item mapping for the lava bucket (bucket
                // recovery was added in 1.8), so supply the empty bucket explicitly.
                if (container == null && fuel.getItem() == Items.lava_bucket) {
                    container = new ItemStack(Items.bucket);
                }
                if (container != null) {
                    container = container.copy();
                }
            }
        }

        ItemStack consumed = inventory.decrStackSize(slot, amount);

        if (burn > 0) {
            this.itemCookTime = Math.max(1, burn / 5);
            if (container != null && container.stackSize > 0) {
                fuzziTweaks$returnContainer(slot, container);
            }
        }

        return consumed;
    }

    /**
     * Puts a consumed fuel's container back into the fuel slot when it is empty. Stacked
     * container fuels leave the remaining fuel in the slot, so the container is dropped at
     * the generator instead of being voided.
     */
    @Unique
    private void fuzziTweaks$returnContainer(int slot, ItemStack container) {
        final IInventory inventory = (IInventory) this;
        ItemStack remaining = inventory.getStackInSlot(slot);

        if (remaining == null) {
            inventory.setInventorySlotContents(slot, container);
            return;
        }

        if (remaining.isItemEqual(container) && ItemStack.areItemStackTagsEqual(remaining, container)) {
            int maxStackSize = Math.min(remaining.getMaxStackSize(), inventory.getInventoryStackLimit());
            if (remaining.stackSize + container.stackSize <= maxStackSize) {
                remaining.stackSize += container.stackSize;
                return;
            }
        }

        this.getWorldObj()
            .spawnEntityInWorld(
                new EntityItem(
                    this.getWorldObj(),
                    this.xCoord + 0.5D,
                    this.yCoord + 0.5D,
                    this.zCoord + 0.5D,
                    container));
    }
}

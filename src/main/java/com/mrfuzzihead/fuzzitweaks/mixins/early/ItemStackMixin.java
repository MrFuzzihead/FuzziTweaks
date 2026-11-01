package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.common.util.EnchantmentSorter;

/**
 * Sorts the enchantment lines of a normal item tooltip without changing {@code getEnchantmentTagList} itself.
 *
 * <p>
 * The getter is public API: other mods read it and some mutate the returned list (anvil logic, NBT editors). Returning
 * a sorted copy from the getter, as LegacyFixes did, silently discards those mutations. Wrapping only the call inside
 * {@code getTooltip} keeps the real list intact and limits the sorted copy to the tooltip render.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @WrapOperation(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getEnchantmentTagList()Lnet/minecraft/nbt/NBTTagList;"),
        require = 1)
    private NBTTagList fuzziTweaks$sortTooltipEnchantments(ItemStack stack, Operation<NBTTagList> original) {
        return EnchantmentSorter.sortedCopy(original.call(stack));
    }
}

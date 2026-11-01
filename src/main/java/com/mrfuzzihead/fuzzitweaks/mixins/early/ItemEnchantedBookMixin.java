package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.common.util.EnchantmentSorter;

/**
 * Sorts the stored-enchantment lines of an enchanted book tooltip.
 *
 * <p>
 * {@code func_92110_g} is also used by {@code addEnchantment} to mutate the real {@code StoredEnchantments} list, so
 * this wraps only the call in {@code addInformation} rather than changing what the getter returns. That leaves
 * book-merging and NBT editing untouched while the tooltip gets the sorted view.
 */
@Mixin(ItemEnchantedBook.class)
public abstract class ItemEnchantedBookMixin {

    @WrapOperation(
        method = "addInformation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemEnchantedBook;func_92110_g(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagList;"),
        require = 1)
    private NBTTagList fuzziTweaks$sortStoredEnchantments(ItemEnchantedBook book, ItemStack stack,
        Operation<NBTTagList> original) {
        return EnchantmentSorter.sortedCopy(original.call(book, stack));
    }
}

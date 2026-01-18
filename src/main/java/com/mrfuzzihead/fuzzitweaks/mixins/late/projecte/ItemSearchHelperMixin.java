package com.mrfuzzihead.fuzzitweaks.mixins.late.projecte;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import moze_intel.projecte.utils.ItemSearchHelper;

@Mixin(ItemSearchHelper.class)
public abstract class ItemSearchHelperMixin {

    /**
     * @author MrFuzzihead
     * @reason Bypassing NEI checker for now to work with GTNH NEI.
     */
    @Overwrite(remap = false)
    public static ItemSearchHelper create(String searchString) {
        try {
            Class<?> defaultSearchClass = Class.forName("moze_intel.projecte.utils.ItemSearchHelper$DefaultSearch");
            java.lang.reflect.Constructor<?> ctor = defaultSearchClass.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return (ItemSearchHelper) ctor.newInstance(searchString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate DefaultSearch via reflection", e);
        }
    }
}

package com.mrfuzzihead.fuzzitweaks.mixins.late.projecte;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import moze_intel.projecte.utils.ItemSearchHelperNEI;

/**
 * ProjectE 1.7.10 builds its NEI search filter from the deprecated
 * {@code SearchField.searchProviders} list. GTNH's NEI search parser no longer
 * populates that list, so every ProjectE search matches everything. Use the
 * parser's own filter instead, which restores NEI syntax ({@code @mod},
 * {@code $ore}, regex, negation, etc.) for ProjectE's transmutation search.
 */
@Mixin(ItemSearchHelperNEI.class)
public abstract class ItemSearchHelperNEIMixin {

    @Unique
    private static boolean fuzziTweaks$legacyNei;

    @Inject(method = "getFilter", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void fuzziTweaks$useNeiSearchParser(String searchText, CallbackInfoReturnable<ItemFilter> cir) {
        if (fuzziTweaks$legacyNei) {
            return;
        }

        try {
            cir.setReturnValue(SearchField.getFilter(searchText));
        } catch (NoSuchMethodError e) {
            // Pre-GTNH NEI has no parser-backed static getFilter; use ProjectE's
            // original provider-list implementation instead.
            fuzziTweaks$legacyNei = true;
        }
    }
}

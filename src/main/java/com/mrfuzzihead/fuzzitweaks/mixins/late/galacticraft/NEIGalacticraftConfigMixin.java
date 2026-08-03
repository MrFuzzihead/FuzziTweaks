package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.api.API;
import micdoodle8.mods.galacticraft.core.nei.ElectricIngotCompressorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.IngotCompressorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.NEIGalacticraftConfig;

/**
 * The GTNH Galacticraft fork commented out the registration of GC's own Ingot Compressor
 * NEI handlers (deferring the machine's recipe display to GregTech's Implosion Compressor,
 * since GC expects GT in a GTNH pack). Re-register them so the GC Compressor recipes show in
 * NEI even when GregTech is not present. The recipe map itself is still populated by GC, so
 * no other change is needed here.
 */
@Mixin(NEIGalacticraftConfig.class)
public abstract class NEIGalacticraftConfigMixin {

    @Inject(method = "loadConfig", at = @At("TAIL"), remap = false)
    private void fuzziTweaks$registerIngotCompressorHandlers(CallbackInfo ci) {
        API.registerRecipeHandler(new IngotCompressorRecipeHandler());
        API.registerUsageHandler(new IngotCompressorRecipeHandler());
        API.registerRecipeHandler(new ElectricIngotCompressorRecipeHandler());
        API.registerUsageHandler(new ElectricIngotCompressorRecipeHandler());
    }
}

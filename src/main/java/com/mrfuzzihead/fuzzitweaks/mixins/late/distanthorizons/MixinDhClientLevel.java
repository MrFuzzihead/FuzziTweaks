package com.mrfuzzihead.fuzzitweaks.mixins.late.distanthorizons;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.seibel.distanthorizons.core.level.AbstractDhLevel;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

/**
 * Mirrors {@link MixinAbstractDhServerLevel} so a player's own client also stops requesting LOD
 * generation from denied/non-allowed dimensions when connected to a remote (possibly unmodified)
 * Distant Horizons-enabled server.
 */
@Mixin(value = DhClientLevel.class, remap = false)
public abstract class MixinDhClientLevel extends AbstractDhLevel {

    @Shadow
    public abstract IClientLevelWrapper getClientLevelWrapper();

    /**
     * @author FuzziTweaks
     * @reason Prevent this client from requesting Distant Horizons LOD generation for dimensions
     *         rejected by the per-dimension allow/deny list (see
     *         {@link Config#distantHorizonsDimensionIds} and
     *         {@link Config#distantHorizonsDimensionListIsDenylist}).
     */
    @Inject(method = "shouldDoWorldGen", at = @At("HEAD"), cancellable = true, remap = false)
    private void fuzzitweaks$filterDimension(CallbackInfoReturnable<Boolean> cir) {
        int dimensionId = (Integer) getClientLevelWrapper().getDimensionType()
            .getWrappedMcObject();
        if (DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(dimensionId)) {
            cir.setReturnValue(false);
        }
    }
}

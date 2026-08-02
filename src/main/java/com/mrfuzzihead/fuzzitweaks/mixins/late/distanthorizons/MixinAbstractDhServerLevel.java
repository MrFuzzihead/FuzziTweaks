package com.mrfuzzihead.fuzzitweaks.mixins.late.distanthorizons;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.DistantHorizonsDimensionFilter;
import com.seibel.distanthorizons.core.level.AbstractDhLevel;
import com.seibel.distanthorizons.core.level.AbstractDhServerLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;

/**
 * Covers both dedicated servers and singleplayer, since {@code DhClientServerLevel} extends this
 * class.
 */
@Mixin(value = AbstractDhServerLevel.class, remap = false)
public abstract class MixinAbstractDhServerLevel extends AbstractDhLevel {

    @Shadow
    public abstract IServerLevelWrapper getServerLevelWrapper();

    /**
     * @author FuzziTweaks
     * @reason Prevent Distant Horizons from generating LOD data in dimensions rejected by the
     *         per-dimension allow/deny list (see {@link Config#distantHorizonsDimensionIds} and
     *         {@link Config#distantHorizonsDimensionListIsDenylist}). This only freezes future
     *         generation for the dimension going forward; any already-generated LOD data is left
     *         untouched on disk and keeps rendering normally.
     */
    @Inject(method = "shouldDoWorldGen", at = @At("HEAD"), cancellable = true, remap = false)
    private void fuzzitweaks$filterDimension(CallbackInfoReturnable<Boolean> cir) {
        int dimensionId = (Integer) getServerLevelWrapper().getDimensionType()
            .getWrappedMcObject();
        if (DistantHorizonsDimensionFilter.isGenerationDeniedInDimension(dimensionId)) {
            cir.setReturnValue(false);
        }
    }
}

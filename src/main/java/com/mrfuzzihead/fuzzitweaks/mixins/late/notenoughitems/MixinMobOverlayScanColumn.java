package com.mrfuzzihead.fuzzitweaks.mixins.late.notenoughitems;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Makes NEI's mob-spawn overlay use the same block-light and sky-light limits as the FuzziTweaks spawn rules.
 *
 * <p>
 * Up to NEI 2.9.7 the overlay scanned blocks in {@code WorldOverlayRenderer#getSpawnMode(Chunk, int, int, int)}.
 * Since NEI 2.9.9 the scan lives in {@code codechicken.nei.moboverlay.MobOverlayScan}, and the spawn mode of a single
 * block is computed by the package-private inner class {@code MobOverlayScan$Column#getSpawnMode(int)}. The overlay
 * itself is cosmetic - a wrong mark only misleads the player - so both injections fail soft ({@code expect = 0}).
 * A future NEI refactor therefore falls back to NEI's stock thresholds instead of crashing the game; the
 * {@code MobOverlayScanColumnMixinTest} unit test guards the target method and its two {@code 8} literals instead.
 *
 * <p>
 * NEI's literals, which are the same as vanilla's:
 *
 * <ul>
 * <li>{@code blockLight >= 8} - no mark at all.</li>
 * <li>{@code skyLight >= 8 ? 1 : 2} - mode 1 is "night only", mode 2 is "any time".</li>
 * </ul>
 */
@Mixin(targets = "codechicken.nei.moboverlay.MobOverlayScan$Column", remap = false)
public abstract class MixinMobOverlayScanColumn {

    /**
     * The block-light cutoff is the first {@code 8} in {@code getSpawnMode}. NEI compares with {@code >=}, and the
     * FuzziTweaks rule rejects light above {@link Config#maxMobBlockLightLevel}, hence the {@code + 1}.
     */
    @ModifyConstant(method = "getSpawnMode", constant = @Constant(intValue = 8, ordinal = 0), expect = 0, require = 0)
    private int fuzziTweaks$maxMobBlockLightLevel(int neiLimit) {
        return Config.maxMobBlockLightLevel + 1;
    }

    /**
     * The sky-light cutoff is the second {@code 8} in {@code getSpawnMode}; NEI marks the block as night only from
     * this value upwards, matching the {@link Config#maxMobSkyLightLevel} + 1 random threshold of a real spawn.
     */
    @ModifyConstant(method = "getSpawnMode", constant = @Constant(intValue = 8, ordinal = 1), expect = 0, require = 0)
    private int fuzziTweaks$maxMobSkyLightLevel(int neiLimit) {
        return Config.maxMobSkyLightLevel + 1;
    }
}

package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * Renders and measures U+202F NARROW NO-BREAK SPACE as an ordinary space.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock), with a width fix added. JDK 20 adopted CLDR 42, which changed the space
 * before the AM/PM marker in {@code DateFormat} output from U+0020 to U+202F. The vanilla 1.7.10 font has no glyph
 * for that code point, so the singleplayer world-selection date renders a missing-glyph box. Remapping the
 * character where {@code renderStringAtPos} reads it makes the space glyph render normally.
 *
 * <p>
 * The original fix only changed rendering, so {@code getStringWidth} still measured U+202F as zero pixels and any
 * centred/right-aligned text was off by one space. {@code getCharWidth} is the single width entry point used by
 * {@code getStringWidth} and the other layout helpers, so returning the vanilla space width there keeps measuring
 * consistent with what is drawn.
 */
@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {

    /** U+202F NARROW NO-BREAK SPACE, used by JDK 20+ CLDR date/time formats before AM/PM. */
    private static final char NARROW_NO_BREAK_SPACE = '\u202F';

    /** Vanilla space width, as returned by {@code FontRenderer#getCharWidth(' ')}. */
    private static final int SPACE_WIDTH = 4;

    @ModifyExpressionValue(
        method = "renderStringAtPos",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;charAt(I)C", ordinal = 0),
        require = 1)
    private char fuzziTweaks$renderNnbspAsSpace(char original) {
        return original == NARROW_NO_BREAK_SPACE ? ' ' : original;
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true, require = 1)
    private void fuzziTweaks$measureNnbspAsSpace(char character, CallbackInfoReturnable<Integer> cir) {
        if (character == NARROW_NO_BREAK_SPACE) {
            cir.setReturnValue(SPACE_WIDTH);
        }
    }
}

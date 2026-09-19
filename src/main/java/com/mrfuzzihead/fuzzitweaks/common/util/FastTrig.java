package com.mrfuzzihead.fuzzitweaks.common.util;

/**
 * Table based {@code atan2} approximation. Used to replace the two
 * {@link Math#atan2(double, double)} calls in {@code EntityLookHelper.onUpdateLook()}, which runs for
 * every living entity on every server tick.
 *
 * <p>
 * Ported from AI Improvements (MIT) by BuiltBrokenModding (DarkCow), which took the algorithm from
 * <a href="http://www.java-gaming.org/index.php?topic=14647.0">java-gaming.org</a>. The 256x256 lookup
 * table has a worst case angular error well under one degree, which is irrelevant for head rotation.
 *
 * <p>
 * The table is built in a static initializer, so no setup call is needed.
 */
public final class FastTrig {

    private static final int ATAN2_BITS = 8;
    private static final int ATAN2_BITS2 = ATAN2_BITS << 1;
    private static final int ATAN2_MASK = ~(-1 << ATAN2_BITS2);
    private static final int ATAN2_COUNT = ATAN2_MASK + 1;
    private static final int ATAN2_DIM = (int) Math.sqrt(ATAN2_COUNT);
    private static final float INV_ATAN2_DIM_MINUS_1 = 1.0f / (ATAN2_DIM - 1);
    private static final float[] ATAN2 = new float[ATAN2_COUNT];

    static {
        for (int i = 0; i < ATAN2_DIM; i++) {
            for (int j = 0; j < ATAN2_DIM; j++) {
                final float x0 = (float) i / ATAN2_DIM;
                final float y0 = (float) j / ATAN2_DIM;
                ATAN2[j * ATAN2_DIM + i] = (float) Math.atan2(y0, x0);
            }
        }
    }

    private FastTrig() {}

    /**
     * Approximates {@link Math#atan2(double, double)}, returning a value in {@code [-PI, PI]}.
     *
     * <p>
     * Edge cases are safe: {@code x == y == 0} produces a {@code NaN} index which Java truncates to
     * {@code 0}, yielding {@code atan2(0, 0) == 0}.
     *
     * <p>
     * Faithful to the original: a negative zero {@code y} is not detected by the {@code y < 0} sign test,
     * so {@code atan2(-0.0, -1.0)} returns {@code +PI} where {@link Math#atan2(double, double)} returns
     * {@code -PI}. The two are the same angle modulo {@code 2*PI}, and the only caller feeds the result
     * through {@code MathHelper.wrapAngleTo180_float}, so the difference is invisible in game.
     */
    public static float atan2(double y, double x) {
        final float add;
        final float mul;

        if (x < 0.0D) {
            if (y < 0.0D) {
                x = -x;
                y = -y;
                mul = 1.0f;
            } else {
                x = -x;
                mul = -1.0f;
            }

            add = -3.141592653f;
        } else {
            if (y < 0.0D) {
                y = -y;
                mul = -1.0f;
            } else {
                mul = 1.0f;
            }

            add = 0.0f;
        }

        final double invDiv = 1.0f / (((x < y) ? y : x) * INV_ATAN2_DIM_MINUS_1);

        final int xi = (int) (x * invDiv);
        final int yi = (int) (y * invDiv);

        return (ATAN2[yi * ATAN2_DIM + xi] + add) * mul;
    }
}

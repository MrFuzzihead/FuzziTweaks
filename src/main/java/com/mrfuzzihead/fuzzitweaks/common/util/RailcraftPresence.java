package com.mrfuzzihead.fuzzitweaks.common.util;

import cpw.mods.fml.common.Loader;

/**
 * Caches whether Railcraft is present after mod construction has finished.
 *
 * <p>
 * The minecart mixin is an early mixin, so its class can be loaded before every mod has registered. A lazy
 * {@link Loader#isModLoaded(String)} check on the first right-click would normally be safe, but a mod that posts a
 * synthetic interaction during pre-init could cache a false result before Railcraft is visible. Initializing this
 * cache from the proxy's pre-init hook avoids that ordering trap, while the fallback keeps the check correct if the
 * mixin is somehow invoked before pre-init.
 * </p>
 */
public final class RailcraftPresence {

    private static volatile Boolean loaded;

    private RailcraftPresence() {}

    /** Called once after all mods have been constructed. */
    public static void init() {
        loaded = Loader.isModLoaded("Railcraft");
    }

    /** @return whether Railcraft is loaded; uncached until {@link #init()} has run. */
    public static boolean isLoaded() {
        final Boolean cached = loaded;
        return cached != null ? cached : Loader.isModLoaded("Railcraft");
    }
}

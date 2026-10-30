package com.mrfuzzihead.fuzzitweaks.common.util;

import net.minecraft.item.Item;

import mods.railcraft.common.carts.CartUtils;

/**
 * Isolates the only compile-time reference to Railcraft.
 *
 * <p>
 * The minecart placement mixin is an early mixin and therefore cannot be gated on a mod ID at load time (early
 * mixins only know which coremods are present, and Railcraft is not a coremod). Keeping the Railcraft access in a
 * separate class means the mixin can check {@link RailcraftPresence} first and only then touch this class, so packs
 * without Railcraft never try to resolve {@link CartUtils}.
 * </p>
 */
public final class RailcraftCartCompat {

    private RailcraftCartCompat() {}

    /**
     * Returns whether Railcraft replaces the given item with one of its own cart types, i.e. whether the item
     * appears in {@link CartUtils#vanillaCartItemMap}.
     */
    public static boolean isReplacedCartItem(Item item) {
        return item != null && CartUtils.vanillaCartItemMap.containsKey(item);
    }
}

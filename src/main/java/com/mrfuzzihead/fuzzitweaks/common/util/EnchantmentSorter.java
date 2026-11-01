package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.google.common.collect.MapMaker;

/**
 * Builds a sorted copy of an enchantment {@link NBTTagList} for tooltip rendering.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock), but deliberately not wired into
 * {@code ItemStack#getEnchantmentTagList} or {@code ItemEnchantedBook#func_92110_g}. Those getters are part of the
 * public item API, and returning a copy from them silently discards mutations made by other mods (anvil code, NBT
 * editors, ...). Instead, the two tooltip call sites use this class and leave the real list untouched.
 *
 * <p>
 * Entries are sorted by the exact string vanilla shows in the tooltip,
 * {@code Enchantment#getTranslatedName(level)}, so the order matches the displayed, localized names and includes the
 * level. Entries that do not resolve to a registered enchantment are omitted from the tooltip copy: vanilla skips a
 * null entry but throws {@code ArrayIndexOutOfBoundsException} for an out-of-range id, so filtering them is both a
 * bounds check and a crash guard.
 *
 * <p>
 * Sorting runs every time the tooltip is drawn, so the result is cached by source-list identity. The cache entry
 * stores a fingerprint of the ids/levels; mutating the source list in place (for example by combining items in an
 * anvil) invalidates it. Keys are weak and identity-based, so a list is only cached while the item/NBT that owns it
 * is alive. The fingerprint's 64-bit hash collision risk is negligible for this UI-only use.
 */
public final class EnchantmentSorter {

    /** Weak identity keys; the sorted copy lives exactly as long as the source NBT list. */
    private static final Map<NBTTagList, Cached> CACHE = new MapMaker().weakKeys()
        .<NBTTagList, Cached>makeMap();

    private EnchantmentSorter() {}

    /**
     * Returns a sorted copy of the given enchantment list, or the input itself when it is null/empty. The returned
     * list is shared between calls until the source contents change and must not be mutated by callers.
     */
    public static NBTTagList sortedCopy(NBTTagList source) {
        if (source == null || source.tagCount() == 0) {
            return source;
        }

        final long fingerprint = fingerprint(source);
        final Cached cached = CACHE.get(source);
        if (cached != null && cached.fingerprint == fingerprint) {
            return cached.sorted;
        }

        final NBTTagList sorted = sort(source);
        CACHE.put(source, new Cached(fingerprint, sorted));
        return sorted;
    }

    private static NBTTagList sort(NBTTagList source) {
        final List<Entry> entries = new ArrayList<>(source.tagCount());
        for (int i = 0; i < source.tagCount(); i++) {
            final NBTTagCompound compound = source.getCompoundTagAt(i);
            final String name = getTranslatedName(compound);
            if (name != null) {
                entries.add(new Entry(compound, name));
            }
        }

        // List#sort is stable, so two entries with the same name keep their NBT order.
        entries.sort(Comparator.comparing(Entry::name));

        final NBTTagList sorted = new NBTTagList();
        for (Entry entry : entries) {
            sorted.appendTag(entry.compound);
        }
        return sorted;
    }

    /**
     * @return the tooltip text for this enchantment entry, or {@code null} when the entry does not resolve to a
     *         registered enchantment
     */
    private static String getTranslatedName(NBTTagCompound compound) {
        final int id = compound.getShort("id");
        if (id < 0 || id >= Enchantment.enchantmentsList.length) {
            return null;
        }

        final Enchantment enchantment = Enchantment.enchantmentsList[id];
        if (enchantment == null) {
            return null;
        }

        return enchantment.getTranslatedName(compound.getShort("lvl"));
    }

    private static long fingerprint(NBTTagList source) {
        // Include the translation update stamp so a language/resource reload invalidates the cached order.
        long hash = 0x9E3779B97F4A7C15L ^ StatCollector.getLastTranslationUpdateTimeInMilliseconds();
        for (int i = 0; i < source.tagCount(); i++) {
            final NBTTagCompound compound = source.getCompoundTagAt(i);
            hash = 31 * hash + compound.getShort("id");
            hash = 31 * hash + compound.getShort("lvl");
        }
        return hash;
    }

    private static final class Entry {

        private final NBTTagCompound compound;
        private final String name;

        Entry(NBTTagCompound compound, String name) {
            this.compound = compound;
            this.name = name;
        }

        String name() {
            return this.name;
        }
    }

    private static final class Cached {

        private final long fingerprint;
        private final NBTTagList sorted;

        Cached(long fingerprint, NBTTagList sorted) {
            this.fingerprint = fingerprint;
            this.sorted = sorted;
        }
    }
}

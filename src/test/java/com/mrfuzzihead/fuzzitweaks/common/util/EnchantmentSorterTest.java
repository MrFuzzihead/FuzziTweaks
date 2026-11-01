package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

/**
 * Covers the parts of {@link EnchantmentSorter} that do not require a running client: the ordering key, invalid-entry
 * filtering, source immutability and the content-fingerprint cache.
 */
class EnchantmentSorterTest {

    @Test
    void sortsByTheDisplayedNameAndDropsUnresolvableEntries() {
        final NBTTagList source = list(Enchantment.sharpness.effectId, 5, Enchantment.protection.effectId, 4, 999, 1);

        final NBTTagList sorted = EnchantmentSorter.sortedCopy(source);

        assertEquals(2, sorted.tagCount());
        final List<String> expected = new ArrayList<>();
        expected.add(Enchantment.protection.getTranslatedName(4));
        expected.add(Enchantment.sharpness.getTranslatedName(5));
        Collections.sort(expected);

        assertEquals(expected.get(0), displayedName(sorted.getCompoundTagAt(0)));
        assertEquals(expected.get(1), displayedName(sorted.getCompoundTagAt(1)));
    }

    @Test
    void leavesTheSourceListUntouchedAndCachesUntilTheContentsChange() {
        final NBTTagList source = list(Enchantment.sharpness.effectId, 5, Enchantment.protection.effectId, 4);

        final NBTTagList first = EnchantmentSorter.sortedCopy(source);
        assertNotSame(source, first);
        assertSame(first, EnchantmentSorter.sortedCopy(source));
        assertEquals(2, source.tagCount());
        assertEquals(
            Enchantment.sharpness.effectId,
            source.getCompoundTagAt(0)
                .getShort("id"));
        assertEquals(
            5,
            source.getCompoundTagAt(0)
                .getShort("lvl"));

        append(source, 34, 3);
        final NBTTagList second = EnchantmentSorter.sortedCopy(source);
        assertNotSame(first, second);
        assertEquals(3, second.tagCount());
    }

    @Test
    void filtersASingleInvalidEntry() {
        assertEquals(
            0,
            EnchantmentSorter.sortedCopy(list(999, 1))
                .tagCount());
        assertEquals(
            0,
            EnchantmentSorter.sortedCopy(list(-1, 1))
                .tagCount());
    }

    @Test
    void handlesNullAndEmptyLists() {
        assertNull(EnchantmentSorter.sortedCopy(null));

        final NBTTagList empty = new NBTTagList();
        assertSame(empty, EnchantmentSorter.sortedCopy(empty));
    }

    private static String displayedName(NBTTagCompound compound) {
        final Enchantment enchantment = Enchantment.enchantmentsList[compound.getShort("id")];
        return enchantment.getTranslatedName(compound.getShort("lvl"));
    }

    private static NBTTagList list(int... idLevelPairs) {
        final NBTTagList list = new NBTTagList();
        for (int i = 0; i + 1 < idLevelPairs.length; i += 2) {
            append(list, idLevelPairs[i], idLevelPairs[i + 1]);
        }
        return list;
    }

    private static void append(NBTTagList list, int id, int level) {
        final NBTTagCompound compound = new NBTTagCompound();
        compound.setShort("id", (short) id);
        compound.setShort("lvl", (short) level);
        list.appendTag(compound);
    }
}

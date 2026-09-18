package com.mrfuzzihead.fuzzitweaks.mixins.late.artifice;

import net.minecraft.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import shukaro.artifice.ArtificeConfig;
import shukaro.artifice.ArtificeCore;
import shukaro.artifice.ArtificeEnchants;
import shukaro.artifice.enchant.EnchantmentInvisible;
import shukaro.artifice.enchant.EnchantmentResistance;
import shukaro.artifice.enchant.EnchantmentSoulstealing;

/**
 * Artifice registers its three enchantments by probing IDs upwards from
 * {@code ArtificeConfig.enchantmentStartID} and relying on the vanilla
 * {@link Enchantment} constructor to throw an {@link IllegalArgumentException}
 * when an ID is already taken. Each enchantment restarts its probe at the same
 * configured start ID.
 *
 * <p>
 * ID-conflict detector coremods replace that constructor with a hook that
 * records the duplicate and overwrites the slot instead of throwing (that is
 * how they can report duplicates at all). With such a constructor in place,
 * Artifice's probe never catches an exception, so every enchantment
 * "successfully" constructs at the same first free ID, silently overwriting
 * the previous entries in {@code enchantmentsList} while still being added to
 * the enchanted-book list. The result is three different enchantments sharing
 * one ID, which shows up as duplicates in ID conflict reports and leaves all
 * but the last registered enchantment broken.
 * </p>
 *
 * <p>
 * This mixin instead picks a genuinely free ID for each enchantment up front
 * from {@link Enchantment#enchantmentsList}, so registration never depends on
 * exception behavior. In a clean (unpatched) environment the IDs assigned are
 * identical to what the original probe produced (lowest free ID at or after
 * the configured start ID, in registration order), so existing worlds are
 * unaffected.
 * </p>
 */
@Mixin(value = ArtificeEnchants.class, remap = false)
public abstract class ArtificeEnchantsMixin {

    @Unique
    private static final int fuzziTweaks$MAX_ENCHANTMENT_ID = Enchantment.enchantmentsList.length;

    /**
     * @author MrFuzzihead
     * @reason The original implementation treated a thrown duplicate-ID
     *         exception from the Enchantment constructor as its only
     *         collision check and restarted every probe at the same start ID.
     *         Whenever that constructor does not throw (e.g. an ID conflict
     *         detector hooks it to record duplicates instead), all three
     *         probes succeed at the same ID and overwrite each other. This
     *         overwrite assigns a verified-free ID to each enchantment before
     *         constructing it, making the assignment deterministic and
     *         duplicate-free regardless of how the constructor behaves.
     */
    @Overwrite(remap = false)
    public static void initEnchants() {
        // IDs claimed during this registration pass, so the three probes can
        // never converge on the same ID even if the constructor does not write
        // the slot into enchantmentsList itself.
        final boolean[] claimed = new boolean[fuzziTweaks$MAX_ENCHANTMENT_ID];

        if (ArtificeConfig.enchantmentInvisibleEnable) {
            final int id = fuzziTweaks$findFreeEnchantID(ArtificeConfig.enchantmentStartID, claimed);
            if (id < 0) {
                ArtificeCore.logger.warn("No available enchantment IDs for invisible enchant!");
            } else {
                claimed[id] = true;
                ArtificeEnchants.enchantmentInvisible = new EnchantmentInvisible(
                    id,
                    ArtificeConfig.enchantmentInvisibleWeight);
                ArtificeCore.logger.info("Registered invisible enchant to ID " + id);
            }
        }

        if (ArtificeConfig.enchantmentSoulstealingEnable) {
            final int id = fuzziTweaks$findFreeEnchantID(ArtificeConfig.enchantmentStartID, claimed);
            if (id < 0) {
                ArtificeCore.logger.warn("No available enchantment IDs for soulstealing enchant!");
            } else {
                claimed[id] = true;
                ArtificeEnchants.enchantmentSoulstealing = new EnchantmentSoulstealing(
                    id,
                    ArtificeConfig.enchantmentSoulstealingWeight);
                ArtificeCore.logger.info("Registered soulstealing enchant to ID " + id);
            }
        }

        if (ArtificeConfig.enchantmentResistanceEnable) {
            final int id = fuzziTweaks$findFreeEnchantID(ArtificeConfig.enchantmentStartID, claimed);
            if (id < 0) {
                ArtificeCore.logger.warn("No available enchantment IDs for resistance enchant!");
            } else {
                claimed[id] = true;
                ArtificeEnchants.enchantmentResistance = new EnchantmentResistance(
                    id,
                    ArtificeConfig.enchantmentResistanceWeight);
                ArtificeCore.logger.info("Registered resistance enchant to ID " + id);
            }
        }
    }

    /**
     * Returns the lowest enchantment ID at or after {@code startID} that is
     * neither occupied in {@link Enchantment#enchantmentsList} nor already
     * claimed during this registration pass, or {@code -1} if none is free.
     */
    @Unique
    private static int fuzziTweaks$findFreeEnchantID(int startID, boolean[] claimed) {
        int id = Math.max(0, startID);
        while (id < fuzziTweaks$MAX_ENCHANTMENT_ID && (Enchantment.enchantmentsList[id] != null || claimed[id])) {
            id++;
        }
        return id < fuzziTweaks$MAX_ENCHANTMENT_ID ? id : -1;
    }
}

package com.mrfuzzihead.fuzzitweaks;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Category used for Minecraft tweak settings. */
    public static final String CATEGORY_MINECRAFT = "minecraft";

    /** Category used for the Distant Horizons per-dimension LOD generation filter settings. */
    public static final String CATEGORY_DISTANT_HORIZONS = "distanthorizons";

    /** Category used for ProjectE tweaks settings. */
    public static final String CATEGORY_PROJECTE = "projecte";

    /** Category used for Thaumic Additions tweaks settings. */
    public static final String CATEGORY_THAUMIC_ADDITIONS = "thaumicadditions";

    /** Category used for Not Enough Items tweaks settings. */
    public static final String CATEGORY_NEI = "nei";

    /** Category used for MCPatcher tweaks settings. */
    public static final String CATEGORY_MCPATCHER = "mcpatcher";

    /** Category used for Galacticraft tweaks settings. */
    public static final String CATEGORY_GALACTICRAFT = "galacticraft";

    /** Category used for Artifice tweaks settings. */
    public static final String CATEGORY_ARTIFICE = "artifice";

    /** Category used for AI tweaks settings (ported from the AI Improvements mod). */
    public static final String CATEGORY_AI = "ai";

    public static boolean enableDespawnModule = true;

    public static boolean enableBackgroundScreenshot = true;

    public static int maxMobBlockLightLevel = 0;

    public static int maxMobSkyLightLevel = 7;

    public static boolean enableProjectETweaks = true;

    public static boolean enableThaumicAdditionsTweaks = true;

    public static boolean enableNEITweaks = true;

    public static boolean enableMCPatcherTweaks = true;

    public static boolean enableGalacticraftCoalGeneratorFuelTweak = true;

    public static boolean enableGalacticraftCompressorNEIFix = true;

    public static boolean enableDistantHorizonsDimensionFilter = true;

    public static boolean enableArtificeEnchantIdFix = true;

    /**
     * Replaces the hot {@code Math.atan2} calls in mob AI (head tracking via {@code EntityLookHelper},
     * {@code EntityLiving.faceEntity}, squid swimming) with
     * {@link com.mrfuzzihead.fuzzitweaks.common.util.FastTrig}. Ported from AI Improvements'
     * {@code ReplaceLookHelper} option, widened to the other hot call sites.
     */
    public static boolean enableFastTrig = true;

    /**
     * Removes the {@code EntityAIWatchClosest} goal (mobs tracking the closest player) from every mob.
     * Ported from AI Improvements' {@code RemoveEntityAIWatchClosest} option.
     */
    public static boolean removeLookAtPlayerGoal = false;

    /**
     * Removes the {@code EntityAILookIdle} goal (mobs looking at random nearby spots) from every mob.
     * Ported from AI Improvements' {@code RemoveEntityAILookIdle} option.
     */
    public static boolean removeLookIdleGoal = false;

    /**
     * Only runs the purely visual look goals ({@code EntityAIWatchClosest} and {@code EntityAILookIdle})
     * while a player is within {@link #lookGoalPlayerRange} blocks. Nobody can see a mob's head movement
     * from further away, so this is a free saving for chunk loaded areas and mob-heavy bases.
     */
    public static boolean onlyRunLookGoalsNearPlayers = true;

    /** Distance in blocks within which a player keeps the visual look goals running. */
    public static int lookGoalPlayerRange = 128;

    /**
     * Fixes the vanilla 1.7.10 melee attack rate bug, where in-range mobs attack every tick instead of
     * once per second. Ported from AI Improvements' attack-on-collide override.
     */
    public static boolean enableMeleeAttackRateFix = true;

    /** Ticks a melee mob has to wait between two attacks. Vanilla 1.8+ uses 20. */
    public static int meleeAttackCooldownTicks = 20;

    public static int[] distantHorizonsDimensionIds = new int[] { 0 };

    /**
     * If {@code true}, {@link #distantHorizonsDimensionIds} is treated as a denylist (every
     * dimension except the listed ones may generate LODs). If {@code false}, it's treated as an
     * allowlist (only the listed dimensions may generate LODs).
     */
    public static boolean distantHorizonsDimensionListIsDenylist = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        enableDespawnModule = configuration.getBoolean(
            "EnableDespawnModule",
            CATEGORY_MINECRAFT,
            true,
            "Enable module that lets mobs holding items/armor despawn");

        maxMobBlockLightLevel = configuration.getInt(
            "MaxMobSpawnLightLevel",
            CATEGORY_MINECRAFT,
            0,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using block light");

        maxMobSkyLightLevel = configuration.getInt(
            "MaxMobSkyLightLevel",
            CATEGORY_MINECRAFT,
            7,
            0,
            15,
            "Highest light level that hostile mobs will spawn at (0-15), using sky light");

        enableBackgroundScreenshot = configuration.getBoolean(
            "EnableBackgroundScreenshot",
            CATEGORY_MINECRAFT,
            true,
            "Take screenshots (F2) off the main thread so the game doesn't freeze while the PNG is written");

        enableProjectETweaks = configuration
            .getBoolean("EnableProjectETweaks", CATEGORY_PROJECTE, true, "Enable fixes and tweaks for ProjectE");

        enableThaumicAdditionsTweaks = configuration.getBoolean(
            "EnableThaumicAdditionsTweaks",
            CATEGORY_THAUMIC_ADDITIONS,
            true,
            "Enable fixes and tweaks for Thaumic Additions");

        enableNEITweaks = configuration
            .getBoolean("EnableNEITweaks", CATEGORY_NEI, true, "Enable fixes and tweaks for Not Enough Items");

        enableMCPatcherTweaks = configuration
            .getBoolean("EnableMCPatcherTweaks", CATEGORY_MCPATCHER, true, "Enable fixes and tweaks for MCPatcher");

        enableGalacticraftCoalGeneratorFuelTweak = configuration.getBoolean(
            "EnableGalacticraftCoalGeneratorFuelTweak",
            CATEGORY_GALACTICRAFT,
            true,
            "Allow the Galacticraft Coal Generator to burn any furnace-burnable item (charcoal, wood, etc.), "
                + "scaled to each item's vanilla furnace burn time.");

        enableGalacticraftCompressorNEIFix = configuration.getBoolean(
            "EnableGalacticraftCompressorNEIFix",
            CATEGORY_GALACTICRAFT,
            true,
            "Re-register GC's Ingot/Electric-Ingot Compressor NEI handlers. The GTNH Galacticraft fork comments "
                + "these out (it expects GregTech's Implosion Compressor to show them), so the GC Compressor "
                + "recipes never appear in NEI when GregTech isn't present.");

        enableDistantHorizonsDimensionFilter = configuration.getBoolean(
            "EnableDistantHorizonsDimensionFilter",
            CATEGORY_DISTANT_HORIZONS,
            true,
            "Enable the per-dimension allow/deny list for Distant Horizons LOD generation");

        distantHorizonsDimensionIds = configuration.get(
            CATEGORY_DISTANT_HORIZONS,
            "DimensionIds",
            new int[0],
            "Dimension IDs this list applies to, see DimensionListIsDenylist. Vanilla: 0=Overworld, -1=Nether, 1=End; "
                + "modded dimensions use their own IDs.")
            .getIntList();

        distantHorizonsDimensionListIsDenylist = configuration.getBoolean(
            "DimensionListIsDenylist",
            CATEGORY_DISTANT_HORIZONS,
            true,
            "If true, DimensionIds is a denylist: Distant Horizons generates LODs everywhere except those dimensions. "
                + "If false, DimensionIds is an allowlist: Distant Horizons only generates LODs in those dimensions.");

        enableArtificeEnchantIdFix = configuration.getBoolean(
            "EnableArtificeEnchantIdFix",
            CATEGORY_ARTIFICE,
            true,
            "Fixes Artifice registering all of its enabled enchantments on the same enchantment ID "
                + "when an ID conflict detector replaces the vanilla Enchantment constructor "
                + "(its registration loop only detected collisions via that constructor throwing). "
                + "Assigns a verified-free ID to each enchantment instead.");

        enableFastTrig = configuration.getBoolean(
            "EnableFastTrig",
            CATEGORY_AI,
            true,
            "Replace the hot Math.atan2 calls in mob AI (head tracking in EntityLookHelper, EntityLiving"
                + ".faceEntity, and squid swimming) with a lookup table approximation. The result differs by "
                + "less than a degree, which is invisible for head and body rotation, and it is cheaper with "
                + "many entities. Ported from AI Improvements' ReplaceLookHelper option.");

        removeLookAtPlayerGoal = configuration.getBoolean(
            "RemoveLookAtPlayerGoal",
            CATEGORY_AI,
            false,
            "Remove the EntityAIWatchClosest goal (mobs turning their head towards the closest player) from "
                + "every mob, including modded subclasses such as EntityAIWatchClosest2. Visual only, but it "
                + "also disables head tracking. Ported from AI Improvements' RemoveEntityAIWatchClosest option.");

        removeLookIdleGoal = configuration.getBoolean(
            "RemoveLookIdleGoal",
            CATEGORY_AI,
            false,
            "Remove the EntityAILookIdle goal (mobs looking at random nearby spots) from every mob. Visual "
                + "only, but it also disables idle head movement. Ported from AI Improvements' "
                + "RemoveEntityAILookIdle option.");

        onlyRunLookGoalsNearPlayers = configuration.getBoolean(
            "OnlyRunLookGoalsNearPlayers",
            CATEGORY_AI,
            false,
            "Only run the purely visual look goals (EntityAIWatchClosest and EntityAILookIdle - mobs "
                + "turning their head towards the closest player, or towards random nearby spots) while a "
                + "player is within LookGoalPlayerRange blocks. Further away nothing can be observed, so the "
                + "goals are skipped entirely, which saves AI work in chunk loaded areas, spawn chunks and "
                + "large bases. Unlike RemoveLookAtPlayerGoal/RemoveLookIdleGoal this keeps head tracking "
                + "normal for every mob a player can actually see.");

        lookGoalPlayerRange = configuration.getInt(
            "LookGoalPlayerRange",
            CATEGORY_AI,
            128,
            1,
            512,
            "Distance in blocks within which a player keeps the visual look goals running when "
                + "OnlyRunLookGoalsNearPlayers is true. The default of 128 is far beyond the range at which "
                + "head movement can be noticed, so leaving it alone is safe; lower values save more CPU but "
                + "make distant mobs ignore players.");

        enableMeleeAttackRateFix = configuration.getBoolean(
            "EnableMeleeAttackRateFix",
            CATEGORY_AI,
            true,
            "Fixes a vanilla 1.7.10 bug in EntityAIAttackOnCollide: in range, mobs swing and attack every "
                + "tick instead of once per second, which roughly doubles melee damage output. Fixing it "
                + "matches the behaviour of Minecraft 1.8 and later. Ported from AI Improvements' "
                + "AttackOnCollide override.");

        meleeAttackCooldownTicks = configuration.getInt(
            "MeleeAttackCooldownTicks",
            CATEGORY_AI,
            20,
            1,
            1200,
            "Ticks a melee mob waits between two attacks when EnableMeleeAttackRateFix is on "
                + "(20 = 1 second, the 1.8+ vanilla value).");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}

package com.mrfuzzihead.fuzzitweaks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Canonical path of the config file that has already been loaded, if any. */
    private static File synchronizedConfigurationFile;

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

    /** Category used for Hats tweaks settings. */
    public static final String CATEGORY_HATS = "hats";

    /** Category used for AI tweaks settings (ported from the AI Improvements mod). */
    public static final String CATEGORY_AI = "ai";

    /** Category used for mob lead (leash) settings. */
    public static final String CATEGORY_LEASH = "leash";

    /** Category used for squid spawn control (ported from Squidless). */
    public static final String CATEGORY_SQUID = "squid";

    /** Category used for the Forge dormant-chunk cache fix (ported from TileEntity Reset Fix). */
    public static final String CATEGORY_TILEENTITY_RESET = "tileentityreset";

    /** Category used for the defensive tile-entity cast fix (ported from Careful Cast Corrector). */
    public static final String CATEGORY_CAREFUL_CAST = "carefulcast";

    /** Category used for Railcraft compatibility fixes. */
    public static final String CATEGORY_RAILCRAFT = "railcraft";

    public static boolean enableDespawnModule = true;

    public static boolean enableBackgroundScreenshot = true;

    public static int maxMobBlockLightLevel = 0;

    public static int maxMobSkyLightLevel = 7;

    public static boolean enableProjectETweaks = true;

    /**
     * Main switch for all Thaumic Additions mixins. Individual tweaks still have their own switches.
     */
    public static boolean enableThaumicAdditionsTweaks = true;

    public static boolean disableThaumicAdditionsEatingGraphic = true;

    public static boolean disableThaumicAdditionsVersionNotifier = true;

    public static boolean enableNEITweaks = true;

    public static boolean enableMCPatcherTweaks = true;

    /**
     * Main switch for all Galacticraft mixins. Individual Galacticraft tweaks still have their own switches.
     */
    public static boolean enableGalacticraftMain = true;

    public static boolean enableGalacticraftCoalGeneratorFuelTweak = true;

    public static boolean enableGalacticraftCompressorNEIFix = true;

    public static boolean enableGalacticraftRocketNEIFix = true;

    public static boolean enableDistantHorizonsDimensionFilter = true;

    public static boolean enableArtificeEnchantIdFix = true;

    /**
     * Repoints the Hats mod's hat download at a live mirror. Hats reads its hat manifest from a hardcoded
     * {@code http://www.creeperrepo.net/ichun/static/hats.xml}; that domain has expired and now returns a
     * parked JavaScript page, so the manifest parse throws inside the reader thread, the exception is swallowed,
     * and the game loads with zero hats (no player hats, no mobs wearing hats, empty selection GUI). The same
     * content is still served by CreeperHost's successor CDN, see {@link #hatsDownloadBaseUrl}.
     */
    public static boolean enableHatsEndpointFix = true;

    /**
     * {@code scheme://host} used for Hats hat downloads. Only URLs that already point at a known Hats
     * repository host (creeperrepo.net, redstone.tech, dist.creeper.host, filedist.ch) get their host swapped
     * for this value, the path is kept as-is, so the manifest and every hat file land on the same layout on the
     * new host. Rewriting the per-file URLs as well is required: the URLs inside the manifest point at
     * {@code http://cdn.redstone.tech}, which redirects to HTTPS, and {@code HttpURLConnection} does not follow
     * protocol-changing redirects. Set this to your own mirror, or leave it blank to disable the rewrite.
     * The value is trimmed, stripped of trailing slashes, and validated when the config loads; an invalid value
     * is treated as blank.
     */
    public static String hatsDownloadBaseUrl = "http://dist.creeper.host";

    /**
     * Reads the modded-mob hat placement list ({@code HatModMobSupport.json}) from the local {@code hats/}
     * folder when that file exists. Hats otherwise fetches it from a raw GitHub path that now returns 404, which
     * leaves modded mobs without hat placement data. Turning this off keeps the original online lookup.
     */
    public static boolean preferLocalHatMobSupport = true;

    /**
     * Main switch for the AI Improvements mixins, the folded Squidless mixins, and the look-goal stripper.
     * Individual tweaks still have their own switches.
     */
    public static boolean enableAIImprovementsMain = true;

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
     * Prevents squids from spawning further than {@link #squidSpawnPlayerRange} blocks from every player.
     * Ported from the Squidless mod; loaded under the shared AI Improvements gate.
     */
    public static boolean enableSquidSpawnControl = true;

    /** Distance in blocks a player must be within for squids to spawn. Squidless used 64. */
    public static int squidSpawnPlayerRange = 64;

    /**
     * Maximum size of a squid spawn pack. {@code 0} keeps vanilla (4); {@code 2} is what Squidless intended but
     * never achieved, because it set {@code maxPackSize} without calling {@code setResult(ALLOW)} and Forge then
     * discarded the value. The shared AI Improvements gate loads the squid pack mixin; this value gates its
     * handler.
     */
    public static int squidMaxPackSize = 2;

    /**
     * Invalidates a Forge dormant chunk as soon as it is served. Ported from TileEntity Reset Fix; only has an
     * effect when {@code forgeChunkLoading.cfg} sets {@code dormantChunkCacheSize} above 0.
     */
    public static boolean enableTileEntityResetFix = true;

    /**
     * Skips the vanilla chest/dungeon tile entity code instead of throwing a {@code ClassCastException} when
     * another mod has replaced the tile entity at that position. Ported from Careful Cast Corrector.
     */
    public static boolean enableCarefulCastCorrector = true;

    /**
     * Re-asserts {@code useItem = DENY} on a right-click-block {@code PlayerInteractEvent} whenever the held item
     * is a cart item Railcraft replaces. Railcraft does the same from its own event handler, but another mod can
     * run after it and reset the result, which lets vanilla place the minecart as well and produces two carts
     * from one click. Injecting at the return of {@code ForgeEventFactory.onPlayerInteract} runs after every
     * event handler, so no handler can undo the deny afterwards.
     */
    public static boolean enableRailcraftMinecartPlacementFix = true;

    /**
     * Logs a stack trace for every right-click-block interaction with a minecart item. Intended for diagnosing a
     * mod that resets the event result or calls {@code ForgeEventFactory.onPlayerInteract} a second time, so it is
     * off by default. See the README for how to read the output.
     */
    public static boolean logMinecartPlacementDebug = false;

    /**
     * Fixes the vanilla 1.7.10 melee attack rate bug, where in-range mobs attack every tick instead of
     * once per second. Ported from AI Improvements' attack-on-collide override.
     */
    public static boolean enableMeleeAttackRateFix = true;

    /** Ticks a melee mob has to wait between two attacks. Vanilla 1.8+ uses 20. */
    public static int meleeAttackCooldownTicks = 20;

    /**
     * Keeps a mob from losing its lead to a fence post while its chunk is still being loaded. Vanilla 1.7.10
     * resolves a saved lead on the mob's first tick and drops it (as an item) if no anchor came out of that
     * single tick, which is exactly what happens when a mob is ticked before the chunk holding the fence post
     * is resident. With this on, a saved fence-post anchor is retried for {@link #leashLoadGraceTicks} ticks
     * while its chunk is unavailable. Once the chunk is resident, a valid fence is re-attached and a removed or
     * replaced fence drops the lead as vanilla does. Player/entity UUID anchors and malformed or missing
     * {@code Leash} tags are left to vanilla.
     */
    public static boolean enableLeashLoadFix = true;

    /**
     * Ticks a restored mob may keep its lead while its fence chunk is not loaded or its knot cannot yet be
     * attached. A mob whose fence is resident and valid is re-attached on the first tick and is unaffected;
     * this only bounds the load-order race before an unresolvable lead is dropped as normal. 0 restores
     * vanilla's single-tick behaviour.
     */
    public static int leashLoadGraceTicks = 200;

    /**
     * Dimension IDs this list applies to. The default is empty so the enabled filter preserves Distant
     * Horizons' normal all-dimension behaviour; see {@link #distantHorizonsDimensionListIsDenylist} for how an
     * empty list is interpreted in denylist and allowlist mode.
     */
    public static int[] distantHorizonsDimensionIds = new int[0];

    /**
     * If {@code true}, {@link #distantHorizonsDimensionIds} is treated as a denylist (every
     * dimension except the listed ones may generate LODs). If {@code false}, it's treated as an
     * allowlist (only the listed dimensions may generate LODs).
     */
    public static boolean distantHorizonsDimensionListIsDenylist = true;

    /**
     * Keeps the Hats URL handling path cheap: trim the configured base once, remove the trailing slashes that
     * would otherwise leave a double slash before the copied path, and reject values the URL parser cannot use.
     * A malformed value is treated like a blank one, which disables rewriting and leaves the original Hats URL
     * untouched.
     */
    private static String normalizeHatsDownloadBaseUrl(String configured) {
        if (configured == null) {
            return "";
        }

        String base = configured.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return "";
        }

        try {
            new URL(base);
        } catch (MalformedURLException e) {
            return "";
        }
        return base;
    }

    /**
     * Resolves the config file used by both mixin selection and runtime settings. The early/late mixin loaders
     * call this before FML has built its pre-init event, so they pass {@code null} and use LaunchWrapper's
     * {@code minecraftHome}. Pre-init passes FML's suggested file; canonicalising both spellings means the normal
     * case compares equal and the file is only read once.
     */
    public static File resolveConfigurationFile(File suggestedFile) {
        File file = suggestedFile != null ? suggestedFile : new File(Launch.minecraftHome, "config/fuzzitweaks.cfg");
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    /**
     * Loads the config once per resolved path. The mixin loaders and pre-init all go through
     * {@link #resolveConfigurationFile(File)}; repeat calls for the same file are no-ops, so the mixin gates and
     * the runtime settings always see the same values without repeatedly parsing and saving the file.
     */
    public static synchronized void synchronizeConfiguration(File configFile) {
        final File resolvedConfigFile = resolveConfigurationFile(configFile);
        if (resolvedConfigFile.equals(synchronizedConfigurationFile)) {
            return;
        }

        Configuration configuration = new Configuration(resolvedConfigFile);

        enableDespawnModule = configuration.getBoolean(
            "EnableDespawnModule",
            CATEGORY_MINECRAFT,
            true,
            "Enable module that lets mobs holding items/armor despawn; picked-up equipment is dropped on despawn");

        maxMobBlockLightLevel = configuration.getInt(
            "MaxMobSpawnLightLevel",
            CATEGORY_MINECRAFT,
            0,
            0,
            15,
            "Maximum block light level at which hostile mobs can spawn (0-15). Modern Minecraft uses 0");

        maxMobSkyLightLevel = configuration.getInt(
            "MaxMobSkyLightLevel",
            CATEGORY_MINECRAFT,
            7,
            0,
            15,
            "Maximum effective sky-adjusted light level for the random hostile-mob spawn check (0-15); "
                + "7 matches modern Minecraft. Lower values make spawns rarer");

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

        disableThaumicAdditionsEatingGraphic = configuration.getBoolean(
            "DisableThaumicAdditionsEatingGraphic",
            CATEGORY_THAUMIC_ADDITIONS,
            true,
            "Hides the green/red item-use progress bar while eating or using an item. The Adaminite "
                + "gauge, tip messages and item HUD extras are left alone");

        disableThaumicAdditionsVersionNotifier = configuration.getBoolean(
            "DisableThaumicAdditionsVersionNotifier",
            CATEGORY_THAUMIC_ADDITIONS,
            true,
            "Suppresses Thaumic Additions' login-time version/update chat messages (outdated, what's "
                + "new, up to date, or no connection)");

        enableNEITweaks = configuration
            .getBoolean("EnableNEITweaks", CATEGORY_NEI, true, "Enable fixes and tweaks for Not Enough Items");

        enableMCPatcherTweaks = configuration
            .getBoolean("EnableMCPatcherTweaks", CATEGORY_MCPATCHER, true, "Enable fixes and tweaks for MCPatcher");

        enableGalacticraftMain = configuration.getBoolean(
            "EnableGalacticraftMain",
            CATEGORY_GALACTICRAFT,
            true,
            "Main switch for all Galacticraft tweaks. When disabled, none of the Galacticraft mixins are applied. "
                + "Individual Galacticraft tweaks still have their own settings below.");

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
                + "recipes never appear in NEI when GregTech isn't present. This does not control rocket recipes.");

        enableGalacticraftRocketNEIFix = configuration.getBoolean(
            "EnableGalacticraftRocketNEIFix",
            CATEGORY_GALACTICRAFT,
            // The old combined option gated both features. Inherit its value on upgrade so users who
            // disabled it do not silently get the rocket recipes back.
            enableGalacticraftCompressorNEIFix,
            "Re-register GC's NASA Workbench rocket NEI handlers (Tier 1-3 and Cargo) and populate their recipe "
                + "displays, which the GTNH fork leaves empty without its coremod. This does not control "
                + "compressor recipes.");

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
                + "modded dimensions use their own IDs. An empty list filters nothing in denylist mode and everything "
                + "in allowlist mode.")
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

        enableHatsEndpointFix = configuration.getBoolean(
            "EnableHatsEndpointFix",
            CATEGORY_HATS,
            true,
            "Repoint the Hats mod's hat download at a live mirror. Hats reads its hat manifest from a hardcoded "
                + "http://www.creeperrepo.net/ichun/static/hats.xml; that domain has expired and now answers with "
                + "a parked JavaScript page, so the manifest parse throws inside the reader thread, the exception "
                + "is swallowed, and the game loads with zero hats (no player hats, no mobs wearing hats, empty "
                + "selection GUI). The same content is still served by CreeperHost's successor CDN, see "
                + "HatDownloadBaseUrl.");

        hatsDownloadBaseUrl = normalizeHatsDownloadBaseUrl(
            configuration.getString(
                "HatDownloadBaseUrl",
                CATEGORY_HATS,
                "http://dist.creeper.host",
                "scheme://host used for Hats hat downloads. Only URLs that already point at a known Hats repository "
                    + "host (creeperrepo.net, redstone.tech, dist.creeper.host, filedist.ch) get their host swapped "
                    + "for this value and the path is kept as-is, so the manifest and every hat file land on the same "
                    + "layout on the new host. Rewriting the per-file URLs is required too: the URLs inside the "
                    + "manifest point at http://cdn.redstone.tech which redirects to HTTPS, and HttpURLConnection does "
                    + "not follow protocol-changing redirects. Point this at your own mirror to stop depending on "
                    + "third parties, or leave it blank to disable the rewrite. The value is trimmed, stripped of "
                    + "trailing slashes, and validated when the config loads; an invalid value disables the rewrite."));

        preferLocalHatMobSupport = configuration.getBoolean(
            "PreferLocalHatMobSupport",
            CATEGORY_HATS,
            true,
            "Read the modded-mob hat placement list (HatModMobSupport.json) from the local hats/ folder when that "
                + "file exists. Hats otherwise fetches it from a raw GitHub path that now returns 404, which leaves "
                + "modded mobs without hat placement data. Turning this off keeps the original online lookup. The "
                + "file ships inside the Hats jar at assets/hats/mod/HatModMobSupport.json, so copying it next to "
                + "the downloaded hats is enough.");

        enableAIImprovementsMain = configuration.getBoolean(
            "EnableAIImprovementsMain",
            CATEGORY_AI,
            true,
            "Main switch for all AI Improvements tweaks, including the folded Squidless spawn and pack-size "
                + "mixins. When disabled, none of the AI mixins are applied and the look-goal removal listener is "
                + "not registered. Individual tweaks still have their own settings below. The mixin gate is read "
                + "at startup, so changing this requires a game restart.");

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
            true,
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

        enableLeashLoadFix = configuration.getBoolean(
            "EnableLeashLoadFix",
            CATEGORY_LEASH,
            true,
            "Keep a mob from losing its lead to a fence post while its chunk is still being loaded. Vanilla "
                + "1.7.10 resolves a saved lead on the mob's first tick after the chunk is read and drops the lead "
                + "(spawning a lead item) when no anchor came out of that single tick, so a mob that is ticked "
                + "before the chunk holding the fence post is resident silently loses its lead. With this on, a "
                + "saved fence-post anchor is retried for LeashLoadGraceTicks ticks while its chunk is unavailable. "
                + "Once the chunk is resident, a valid fence is re-attached and a removed or replaced fence drops "
                + "the lead as vanilla does. Player/entity UUID anchors and malformed or missing Leash tags are "
                + "left to vanilla. A lead knot is also not killed while its fence chunk is not loaded.");

        leashLoadGraceTicks = configuration.getInt(
            "LeashLoadGraceTicks",
            CATEGORY_LEASH,
            200,
            0,
            1200,
            "Ticks a mob restored from NBT may keep its lead while its fence chunk is not loaded or its knot "
                + "cannot yet be attached when EnableLeashLoadFix is on. A mob whose fence is resident and valid "
                + "is re-attached on the first tick and is unaffected by this; it only bounds the load-order race "
                + "before an unresolvable lead is dropped as normal. 0 restores vanilla's single-tick behaviour. "
                + "200 is 10 seconds, far more than a chunk load needs.");

        enableSquidSpawnControl = configuration.getBoolean(
            "EnableSquidSpawnControl",
            CATEGORY_SQUID,
            true,
            "Prevent squids from spawning further than SquidSpawnPlayerRange blocks from every player. "
                + "Ported from the Squidless mod.");

        squidSpawnPlayerRange = configuration.getInt(
            "SquidSpawnPlayerRange",
            CATEGORY_SQUID,
            64,
            1,
            256,
            "Distance in blocks a player must be within for squids to spawn. Squidless used 64.");

        squidMaxPackSize = configuration.getInt(
            "SquidMaxPackSize",
            CATEGORY_SQUID,
            2,
            0,
            64,
            "Maximum size of a squid spawn pack. 0 keeps vanilla (4). The default of 2 is what Squidless "
                + "intended but never achieved: it set maxPackSize without calling setResult(ALLOW), so Forge "
                + "discarded the value and vanilla's 4 was always used. This port returns the value "
                + "directly, so 2 now actually takes effect. Set to 0 to keep vanilla pack sizes.");

        enableTileEntityResetFix = configuration.getBoolean(
            "EnableTileEntityResetFix",
            CATEGORY_TILEENTITY_RESET,
            true,
            "Invalidate a Forge dormant chunk as soon as it is served, so an already unloaded chunk object "
                + "cannot be reused and reset tile entity data. Only has an effect when forgeChunkLoading.cfg "
                + "sets dormantChunkCacheSize above 0.");

        enableCarefulCastCorrector = configuration.getBoolean(
            "EnableCarefulCastCorrector",
            CATEGORY_CAREFUL_CAST,
            true,
            "Skip the vanilla chest/dungeon tile entity code instead of throwing a ClassCastException when "
                + "another mod has replaced the tile entity at that position. Ported from Careful Cast Corrector.");

        enableRailcraftMinecartPlacementFix = configuration.getBoolean(
            "EnableRailcraftMinecartPlacementFix",
            CATEGORY_RAILCRAFT,
            true,
            "Re-assert useItem = DENY on right-click-block PlayerInteractEvents for Railcraft-replaced cart "
                + "items. Railcraft already denies the item use from its own event handler, but another mod can "
                + "reset the result after it and let vanilla place a second minecart. Running after every event "
                + "handler cannot be undone and leaves only the Railcraft cart.");

        logMinecartPlacementDebug = configuration.getBoolean(
            "LogMinecartPlacementDebug",
            CATEGORY_RAILCRAFT,
            false,
            "Print a stack trace for every right-click-block interaction with a minecart item. The trace names "
                + "the caller of ForgeEventFactory.onPlayerInteract. Two traces for one click mean another mod "
                + "calls onPlayerInteract a second time; a single trace with useItemBefore=DEFAULT means a handler "
                + "reset the result, or the event was canceled before Railcraft's handler ran. A synthetic event "
                + "posted directly to the event bus does not appear as an onPlayerInteract trace. Intended for "
                + "diagnosis, so it is off by default.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
        synchronizedConfigurationFile = resolvedConfigFile;
    }
}

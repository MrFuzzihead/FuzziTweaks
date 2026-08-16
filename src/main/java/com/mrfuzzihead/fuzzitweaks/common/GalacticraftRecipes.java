package com.mrfuzzihead.fuzzitweaks.common;

import java.util.HashMap;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.items.GCItems;
import micdoodle8.mods.galacticraft.core.recipe.NasaWorkbenchRecipe;
import micdoodle8.mods.galacticraft.core.util.RecipeUtil;
import micdoodle8.mods.galacticraft.planets.asteroids.items.AsteroidsItems;
import micdoodle8.mods.galacticraft.planets.mars.items.MarsItems;
import micdoodle8.mods.galacticraft.planets.mars.util.MarsUtil;

/**
 * Restores Galacticraft crafting that is missing or gated in the GTNH Galacticraft
 * fork (which this pack does not sit on top of - no NewHorizonsCoreMod, no GregTech):
 *
 * <ul>
 * <li>The NASA Workbench block - the fork ships no recipe for it at all.</li>
 * <li>The Tier 1 rocket - its NASA Workbench recipe was removed and "moved to the
 * coremod", which we don't have, so it was never re-added.</li>
 * <li>The medium, heavy, super-heavy and ultra oxygen tanks - GC itself only crafts the
 * small (light) tank; the bigger ones depended on the coremod's (GregTech) recipes.
 * All five tiers are re-crafted here into a consistent material ladder.</li>
 * </ul>
 *
 * <p>
 * All of these register at {@link FMLPostInitializationEvent} time (see
 * {@link com.mrfuzzihead.fuzzitweaks.FuzziTweaks#postInit}) so they run after
 * Galacticraft and any recipe-removal passes. Ingredients are GC-native (ores,
 * compressed plates, canisters) so they work without GregTech/the coremod.
 */
public final class GalacticraftRecipes {

    private GalacticraftRecipes() {}

    /** Entry point, invoked from FuzziTweaks.postInit when GalacticraftCore is loaded. */
    public static void register() {
        addNasaWorkbenchBlockRecipe();
        addTier1RocketRecipe();
        addTier2RocketRecipe();
        addTier3RocketRecipe();
        addCargoRocketRecipe();
        addOxygenTankRecipes();
    }

    /**
     * The NASA Workbench block has no crafting recipe in the GTNH fork.
     * GC-native recipe:
     *
     * <pre>
     *   S C S        S = Compressed Steel (corners)
     *   L W L        C = Crafting Table (top middle)
     *   S R S        L = Lever (sides)
     *                W = Advanced Wafer (center)
     *                R = Redstone Torch (bottom middle)
     * </pre>
     */
    private static void addNasaWorkbenchBlockRecipe() {
        RecipeUtil.addRecipe(
            new ItemStack(GCBlocks.nasaWorkbench, 1, 0),
            new Object[] { "SCS", "LWL", "SRS", 'S', "compressedSteel", 'C', Blocks.crafting_table, 'L', Blocks.lever,
                'W', "waferAdvanced", 'R', Blocks.redstone_torch });
    }

    /**
     * Tier 1 rocket is assembled in the NASA Workbench, but the fork removed its
     * recipe. Re-add it using GC's own parts (which GC still crafts without the
     * coremod). Slot layout mirrors {@code ContainerSchematicTier1Rocket} /
     * {@code SlotRocketBench}:
     *
     * <pre>
     *   1        nose cone
     *   2-9      heavy plating tier 1 (body)
     *   10,11    fins (left)
     *   12       rocket engine, meta 0
     *   13,14    fins (right)
     *   15-17    cargo add-ons (empty = basic rocket)
     * </pre>
     */
    private static void addTier1RocketRecipe() {
        final HashMap<Integer, ItemStack> base = new HashMap<>();

        base.put(1, new ItemStack(GCItems.partNoseCone, 1, 0));
        for (int i = 2; i <= 9; i++) {
            base.put(i, new ItemStack(GCItems.heavyPlatingTier1, 1, 0));
        }
        base.put(10, new ItemStack(GCItems.partFins, 1, 0));
        base.put(11, new ItemStack(GCItems.partFins, 1, 0));
        // Meta 0 is the Tier 1 engine (meta 1 is the booster).
        base.put(12, new ItemStack(GCItems.rocketEngine, 1, 0));
        base.put(13, new ItemStack(GCItems.partFins, 1, 0));
        base.put(14, new ItemStack(GCItems.partFins, 1, 0));

        // Cargo capacity is set by which of the three add-on slots (15-17) hold a
        // vanilla chest. Output metas: 0 = basic, 1 = 18-slot, 2 = 36-slot, 3 = 54-slot.
        final ItemStack chest = new ItemStack(Blocks.chest);

        addTier1CargoVariant(base, null, null, null, 0);
        addTier1CargoVariant(base, chest, null, null, 1);
        addTier1CargoVariant(base, null, chest, null, 1);
        addTier1CargoVariant(base, null, null, chest, 1);
        addTier1CargoVariant(base, chest, chest, null, 2);
        addTier1CargoVariant(base, chest, null, chest, 2);
        addTier1CargoVariant(base, null, chest, chest, 2);
        addTier1CargoVariant(base, chest, chest, chest, 3);
    }

    /**
     * Registers one Tier 1 rocket NASA-Workbench recipe with a given cargo-chest layout.
     *
     * @param base the part layout (slots 1-14), copied before the cargo slots are set
     * @param s15  chest in add-on slot 15, or {@code null} to leave it empty
     * @param s16  chest in add-on slot 16, or {@code null} to leave it empty
     * @param s17  chest in add-on slot 17, or {@code null} to leave it empty
     * @param meta the output cargo tier ({@code rocketTier1} item damage)
     */
    private static void addTier1CargoVariant(HashMap<Integer, ItemStack> base, ItemStack s15, ItemStack s16,
        ItemStack s17, int meta) {
        final HashMap<Integer, ItemStack> input = new HashMap<>(base);
        input.put(15, s15);
        input.put(16, s16);
        input.put(17, s17);
        RecipeUtil.addRocketBenchRecipe(new ItemStack(GCItems.rocketTier1, 1, meta), input);
    }

    /**
     * Tier 2 rocket (to Mars) NASA-Workbench recipes, restored for non-coremod packs.
     * Slot layout mirrors {@code ContainerSchematicTier2Rocket} / {@code SlotSchematicTier2Rocket}:
     * nose cone, 10x reinforced plate (Mars), boosters, main engine, fins, then 3 cargo add-on
     * slots (19-21). Basic + 7 cargo variants (metas 0-3 on {@code spaceship}).
     */
    private static void addTier2RocketRecipe() {
        final HashMap<Integer, ItemStack> parts = new HashMap<>();
        parts.put(1, new ItemStack(GCItems.partNoseCone, 1, 0));
        for (int i = 2; i <= 11; i++) {
            parts.put(i, new ItemStack(MarsItems.marsItemBasic, 1, 3)); // reinforced plate T2
        }
        parts.put(12, new ItemStack(GCItems.rocketEngine, 1, 1)); // booster
        parts.put(13, new ItemStack(GCItems.partFins, 1, 0));
        parts.put(14, new ItemStack(GCItems.partFins, 1, 0));
        parts.put(15, new ItemStack(GCItems.rocketEngine, 1, 0)); // main engine
        parts.put(16, new ItemStack(GCItems.rocketEngine, 1, 1)); // booster
        parts.put(17, new ItemStack(GCItems.partFins, 1, 0));
        parts.put(18, new ItemStack(GCItems.partFins, 1, 0));

        addTier23RocketVariants(parts, MarsUtil::addRocketBenchT2Recipe, MarsItems.spaceship);
    }

    /**
     * Tier 3 rocket (to Asteroids) NASA-Workbench recipes. Slot layout mirrors
     * {@code ContainerSchematicTier3Rocket} / {@code SlotSchematicTier3Rocket}:
     * heavy nose cone, 10x reinforced plate T3, boosters, AST engine, AST fins, then 3 cargo
     * add-on slots (19-21). Basic + 7 cargo variants (metas 0-3 on {@code tier3Rocket}).
     */
    private static void addTier3RocketRecipe() {
        final HashMap<Integer, ItemStack> parts = new HashMap<>();
        parts.put(1, new ItemStack(AsteroidsItems.heavyNoseCone, 1, 0));
        for (int i = 2; i <= 11; i++) {
            parts.put(i, new ItemStack(AsteroidsItems.basicItem, 1, 0)); // reinforced plate T3
        }
        parts.put(12, new ItemStack(GCItems.rocketEngine, 1, 1)); // booster
        parts.put(13, new ItemStack(AsteroidsItems.basicItem, 1, 2)); // AST fins
        parts.put(14, new ItemStack(AsteroidsItems.basicItem, 1, 2));
        parts.put(15, new ItemStack(AsteroidsItems.basicItem, 1, 1)); // AST engine
        parts.put(16, new ItemStack(GCItems.rocketEngine, 1, 1)); // booster
        parts.put(17, new ItemStack(AsteroidsItems.basicItem, 1, 2));
        parts.put(18, new ItemStack(AsteroidsItems.basicItem, 1, 2));

        addTier23RocketVariants(
            parts,
            (result, input) -> GalacticraftRegistry.addT3RocketRecipe(new NasaWorkbenchRecipe(result, input)),
            AsteroidsItems.tier3Rocket);
    }

    @FunctionalInterface
    private interface NasaRecipeAdder {

        void add(ItemStack result, HashMap<Integer, ItemStack> input);
    }

    /** Registers all 8 Tier-2/3 rocket variants for a parts layout (basic + 7 cargo). */
    private static void addTier23RocketVariants(HashMap<Integer, ItemStack> parts, NasaRecipeAdder adder,
        Item rocketItem) {
        final ItemStack chest = new ItemStack(Blocks.chest);
        addTier23RocketVariant(parts, adder, rocketItem, 0, null, null, null);
        addTier23RocketVariant(parts, adder, rocketItem, 1, chest, null, null);
        addTier23RocketVariant(parts, adder, rocketItem, 1, null, chest, null);
        addTier23RocketVariant(parts, adder, rocketItem, 1, null, null, chest);
        addTier23RocketVariant(parts, adder, rocketItem, 2, chest, chest, null);
        addTier23RocketVariant(parts, adder, rocketItem, 2, chest, null, chest);
        addTier23RocketVariant(parts, adder, rocketItem, 2, null, chest, chest);
        addTier23RocketVariant(parts, adder, rocketItem, 3, chest, chest, chest);
    }

    private static void addTier23RocketVariant(HashMap<Integer, ItemStack> parts, NasaRecipeAdder adder,
        Item rocketItem, int meta, ItemStack s19, ItemStack s20, ItemStack s21) {
        final HashMap<Integer, ItemStack> input = new HashMap<>(parts);
        input.put(19, s19);
        input.put(20, s20);
        input.put(21, s21);
        adder.add(new ItemStack(rocketItem, 1, meta), input);
    }

    /**
     * Cargo Rocket (transport ship, to Mars) NASA-Workbench recipes, restored to the vanilla
     * (non-GT) layout: Advanced Wafer, Nose Cone, 6 Heavy-Duty Plates T2, a rocket engine, 4
     * fins, and a chest in the cargo slot. Slot layout mirrors
     * {@code ContainerSchematicCargoRocket} / {@code SlotSchematicCargoRocket}. The chest type
     * sets the cargo capacity: chest metas 3/0/1 -> {@code spaceship} metas 11/12/13.
     */
    private static void addCargoRocketRecipe() {
        final HashMap<Integer, ItemStack> base = new HashMap<>();
        base.put(1, new ItemStack(GCItems.basicItem, 1, 14)); // advanced wafer
        base.put(7, new ItemStack(GCItems.partNoseCone, 1, 0));
        // Six Heavy-Duty Plates T2 (valid in slots 8-15; vanilla uses the top three rows).
        for (int i = 8; i <= 13; i++) {
            base.put(i, new ItemStack(MarsItems.marsItemBasic, 1, 3));
        }
        base.put(16, new ItemStack(GCItems.rocketEngine, 1, 0)); // rocket engine T1
        for (int i = 17; i <= 20; i++) {
            base.put(i, new ItemStack(GCItems.partFins, 1, 0));
        }

        final HashMap<Integer, ItemStack> chest3 = new HashMap<>(base);
        chest3.put(21, RecipeUtil.getChestItemStack(1, 3));
        MarsUtil.adCargoRocketRecipe(new ItemStack(MarsItems.spaceship, 1, 11), chest3);

        final HashMap<Integer, ItemStack> chest0 = new HashMap<>(base);
        chest0.put(21, RecipeUtil.getChestItemStack(1, 0));
        MarsUtil.adCargoRocketRecipe(new ItemStack(MarsItems.spaceship, 1, 12), chest0);

        final HashMap<Integer, ItemStack> chest1 = new HashMap<>(base);
        chest1.put(21, RecipeUtil.getChestItemStack(1, 1));
        MarsUtil.adCargoRocketRecipe(new ItemStack(MarsItems.spaceship, 1, 13), chest1);
    }

    /**
     * Rewires the whole oxygen-tank line into a clean, progressive ladder (each tier's
     * metal is available a step before the machine that consumes it):
     *
     * <pre>
     *   Light        Iron Ingot
     *   Medium       Compressed Aluminum
     *   Heavy        Compressed Steel
     *   Super Heavy  Compressed Meteoric Iron
     *   Ultra Heavy  Compressed Desh
     * </pre>
     *
     * Every tier uses the same ring shape (7 plates of the metal + a canister in the
     * center + an oxygen pipe) and outputs an EMPTY tank ({@code damage == maxDamage},
     * refillable in the Oxygen Compressor). Both tin- and copper-canister variants are
     * added for each tier. All five are craftable (GC previously only provided the
     * light tank, and only with compressed aluminum).
     */
    private static void addOxygenTankRecipes() {
        // Drop GC's built-in compressed-aluminum light tank so the ladder is uniform.
        removeRecipesFor(GCItems.oxTankLight);

        addOxygenTankRecipe(GCItems.oxTankLight, "IPI", "ICI", "III", 'I', "ingotIron");
        addOxygenTankRecipe(GCItems.oxTankMedium, "APA", "ACA", "AAA", 'A', "compressedAluminum");
        addOxygenTankRecipe(GCItems.oxTankHeavy, "SPS", "SCS", "SSS", 'S', "compressedSteel");
        addOxygenTankRecipe(GCItems.oxTankSuperHeavy, "MPM", "MCM", "MMM", 'M', "compressedMeteoricIron");
        addOxygenTankRecipe(GCItems.oxTankUltraHeavy, "DPD", "DCD", "DDD", 'D', "compressedDesh");
    }

    /**
     * Removes every registered crafting recipe whose output is the given item.
     * Used to clear GC's default compressed-aluminum light-tank recipe.
     */
    @SuppressWarnings("unchecked")
    private static void removeRecipesFor(Item item) {
        final List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        for (int i = recipes.size() - 1; i >= 0; i--) {
            final IRecipe recipe = recipes.get(i);
            final ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() == item) {
                recipes.remove(i);
            }
        }
    }

    /**
     * Adds both a tin-canister (meta 0) and copper-canister (meta 1) variant of a given
     * oxygen-tank recipe.
     *
     * @param tank   the oxygen tank item (empty tank output, {@code damage == maxDamage})
     * @param top    top grid row, with 'C' as the canister slot and 'P' as the pipe slot
     * @param mid    middle grid row
     * @param bottom bottom grid row
     * @param matKey the ore-dict key's grid character (e.g. 'I', 'A', 'S', 'M', 'D')
     * @param matOre the ore-dict tag for the tank material
     */
    private static void addOxygenTankRecipe(Item tank, String top, String mid, String bottom, char matKey,
        String matOre) {
        for (int canisterMeta = 0; canisterMeta <= 1; canisterMeta++) {
            RecipeUtil.addRecipe(
                new ItemStack(tank, 1, tank.getMaxDamage()),
                new Object[] { top, mid, bottom, matKey, matOre, 'C', new ItemStack(GCItems.canister, 1, canisterMeta),
                    'P', GCBlocks.oxygenPipe });
        }
    }
}

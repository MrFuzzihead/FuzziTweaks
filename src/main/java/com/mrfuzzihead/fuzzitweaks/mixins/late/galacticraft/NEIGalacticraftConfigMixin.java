package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import micdoodle8.mods.galacticraft.core.items.GCItems;
import micdoodle8.mods.galacticraft.core.nei.ElectricIngotCompressorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.IngotCompressorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.NEIGalacticraftConfig;
import micdoodle8.mods.galacticraft.core.nei.RocketT1RecipeHandler;
import micdoodle8.mods.galacticraft.core.util.RecipeUtil;
import micdoodle8.mods.galacticraft.planets.asteroids.items.AsteroidsItems;
import micdoodle8.mods.galacticraft.planets.asteroids.nei.NEIGalacticraftAsteroidsConfig;
import micdoodle8.mods.galacticraft.planets.asteroids.nei.RocketT3RecipeHandler;
import micdoodle8.mods.galacticraft.planets.mars.items.MarsItems;
import micdoodle8.mods.galacticraft.planets.mars.nei.NEIGalacticraftMarsConfig;
import micdoodle8.mods.galacticraft.planets.mars.nei.RocketT2RecipeHandler;

/**
 * The GTNH Galacticraft fork commented out several of GC's own NEI registrations,
 * deferring them to GregTech / the coremod that a GTNH pack would have. In a pack
 * without those, the GC machine/bench recipes would otherwise be invisible in NEI,
 * so this mixin re-registers them at {@code NEIGalacticraftConfig#loadConfig}:
 *
 * <ul>
 * <li>Ingot Compressor handlers (already present).</li>
 * <li>The NASA Workbench Tier-1 rocket handler, plus its recipe display. GC never
 * even populates the {@code rocketBenchRecipes} map itself, so this mixin builds
 * it from the restored Tier-1 rocket recipe and the schematic GUI slot positions.</li>
 * </ul>
 */
@Mixin(NEIGalacticraftConfig.class)
public abstract class NEIGalacticraftConfigMixin {

    @Inject(method = "loadConfig", at = @At("TAIL"), remap = false)
    private void fuzziTweaks$registerIngotCompressorHandlers(CallbackInfo ci) {
        API.registerRecipeHandler(new IngotCompressorRecipeHandler());
        API.registerUsageHandler(new IngotCompressorRecipeHandler());
        API.registerRecipeHandler(new ElectricIngotCompressorRecipeHandler());
        API.registerUsageHandler(new ElectricIngotCompressorRecipeHandler());

        // NASA Workbench rockets: re-register the handlers GC commented out and populate
        // their display maps (they're empty in a non-coremod pack).
        API.registerRecipeHandler(new RocketT1RecipeHandler());
        API.registerUsageHandler(new RocketT1RecipeHandler());
        this.fuzziTweaks$registerTier1Rocket();

        API.registerRecipeHandler(new RocketT2RecipeHandler());
        API.registerUsageHandler(new RocketT2RecipeHandler());
        this.fuzziTweaks$registerTier2Rocket();

        API.registerRecipeHandler(new RocketT3RecipeHandler());
        API.registerUsageHandler(new RocketT3RecipeHandler());
        this.fuzziTweaks$registerTier3Rocket();
        this.fuzziTweaks$registerCargoRocket();
    }

    /**
     * Populates {@link NEIGalacticraftConfig}'s internal rocket-bench display map so the
     * Tier-1 rocket and its cargo variants (restored elsewhere in FuzziTweaks) show up in
     * NEI. Positions are the schematic GUI slot coordinates minus the (3,4) texture crop
     * the {@link RocketT1RecipeHandler} applies when drawing {@code rocketbench.png}.
     *
     * <pre>
     *   1            nose cone
     *   2-9          heavy plating (body)
     *   10,11,13,14  fins
     *   12           engine
     *   15-17        cargo add-ons (vanilla chests)
     * </pre>
     */
    @Unique
    private void fuzziTweaks$registerTier1Rocket() {
        final HashMap<Integer, PositionedStack> base = new HashMap<>();

        base.put(1, new PositionedStack(new ItemStack(GCItems.partNoseCone, 1, 0), 45, 15));
        for (int i = 2; i <= 9; i++) {
            final boolean right = i >= 6;
            final int row = (i - 2) % 4;
            base.put(i, new PositionedStack(new ItemStack(GCItems.heavyPlatingTier1), right ? 54 : 36, 33 + row * 18));
        }
        base.put(10, new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 18, 87));
        base.put(11, new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 18, 105));
        base.put(12, new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 0), 45, 105));
        base.put(13, new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 72, 87));
        base.put(14, new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 72, 105));

        final ItemStack chest = new ItemStack(Blocks.chest);
        this.fuzziTweaks$registerTier1Cargo(base, null, null, null, 0);
        this.fuzziTweaks$registerTier1Cargo(base, chest, null, null, 1);
        this.fuzziTweaks$registerTier1Cargo(base, null, chest, null, 1);
        this.fuzziTweaks$registerTier1Cargo(base, null, null, chest, 1);
        this.fuzziTweaks$registerTier1Cargo(base, chest, chest, null, 2);
        this.fuzziTweaks$registerTier1Cargo(base, chest, null, chest, 2);
        this.fuzziTweaks$registerTier1Cargo(base, null, chest, chest, 2);
        this.fuzziTweaks$registerTier1Cargo(base, chest, chest, chest, 3);
    }

    /**
     * Registers one Tier-1 rocket NEI bench display for a given cargo-chest layout.
     * Add-on slot icon positions (container x/y minus the (3,4) crop): 15=(90,8),
     * 16=(116,8), 17=(142,8). The output bullet sits at (139,92).
     */
    @Unique
    private void fuzziTweaks$registerTier1Cargo(HashMap<Integer, PositionedStack> base, ItemStack s15, ItemStack s16,
        ItemStack s17, int meta) {
        final HashMap<Integer, PositionedStack> input = new HashMap<>(base);
        if (s15 != null) {
            input.put(15, new PositionedStack(s15, 90, 8));
        }
        if (s16 != null) {
            input.put(16, new PositionedStack(s16, 116, 8));
        }
        if (s17 != null) {
            input.put(17, new PositionedStack(s17, 142, 8));
        }
        new NEIGalacticraftConfig().registerRocketBenchRecipe(
            input,
            new PositionedStack(new ItemStack(GCItems.rocketTier1, 1, meta), 139, 92));
    }

    /**
     * Registers the Tier-2 rocket (to Mars) and its 7 cargo variants in NEI.
     * Uses the Mars plugin's list-style rocket-bench map. Positions are the schematic
     * container coords minus the (3, 4 + 8) offset the {@link RocketT2RecipeHandler}
     * applies (it draws at dest (0,-8) from texture crop (3,4)).
     */
    @Unique
    private void fuzziTweaks$registerTier2Rocket() {
        final ArrayList<PositionedStack> base = new ArrayList<>();
        base.add(new PositionedStack(new ItemStack(GCItems.partNoseCone, 1, 0), 45, 7));
        for (int row = 0; row < 5; row++) {
            final int y = 25 + row * 18;
            base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 36, y));
            base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 54, y));
        }
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 1), 18, 79));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 18, 97));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 18, 115));
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 0), 45, 115));
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 1), 72, 79));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 72, 97));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 72, 115));

        final ItemStack chest = new ItemStack(Blocks.chest);
        this.fuzziTweaks$registerTier2Cargo(base, null, null, null, 0);
        this.fuzziTweaks$registerTier2Cargo(base, chest, null, null, 1);
        this.fuzziTweaks$registerTier2Cargo(base, null, chest, null, 1);
        this.fuzziTweaks$registerTier2Cargo(base, null, null, chest, 1);
        this.fuzziTweaks$registerTier2Cargo(base, chest, chest, null, 2);
        this.fuzziTweaks$registerTier2Cargo(base, chest, null, chest, 2);
        this.fuzziTweaks$registerTier2Cargo(base, null, chest, chest, 2);
        this.fuzziTweaks$registerTier2Cargo(base, chest, chest, chest, 3);
    }

    @Unique
    private void fuzziTweaks$registerTier2Cargo(ArrayList<PositionedStack> base, ItemStack s19, ItemStack s20,
        ItemStack s21, int meta) {
        final ArrayList<PositionedStack> input = new ArrayList<>(base);
        if (s19 != null) {
            input.add(new PositionedStack(s19, 90, 0));
        }
        if (s20 != null) {
            input.add(new PositionedStack(s20, 116, 0));
        }
        if (s21 != null) {
            input.add(new PositionedStack(s21, 142, 0));
        }
        new NEIGalacticraftMarsConfig().registerRocketBenchRecipe(
            input,
            new PositionedStack(new ItemStack(MarsItems.spaceship, 1, meta), 139, 102));
    }

    /**
     * Registers the Tier-3 rocket (to Asteroids) and its 7 cargo variants in NEI, using
     * the Asteroids plugin's list-style rocket-bench map (same coordinate convention as
     * the {@link RocketT2RecipeHandler}).
     */
    @Unique
    private void fuzziTweaks$registerTier3Rocket() {
        final ArrayList<PositionedStack> base = new ArrayList<>();
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.heavyNoseCone, 1, 0), 45, 7));
        for (int row = 0; row < 5; row++) {
            final int y = 25 + row * 18;
            base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 0), 36, y));
            base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 0), 54, y));
        }
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 1), 18, 79));
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 2), 18, 97));
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 2), 18, 115));
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 1), 45, 115));
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 1), 72, 79));
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 2), 72, 97));
        base.add(new PositionedStack(new ItemStack(AsteroidsItems.basicItem, 1, 2), 72, 115));

        final ItemStack chest = new ItemStack(Blocks.chest);
        this.fuzziTweaks$registerTier3Cargo(base, null, null, null, 0);
        this.fuzziTweaks$registerTier3Cargo(base, chest, null, null, 1);
        this.fuzziTweaks$registerTier3Cargo(base, null, chest, null, 1);
        this.fuzziTweaks$registerTier3Cargo(base, null, null, chest, 1);
        this.fuzziTweaks$registerTier3Cargo(base, chest, chest, null, 2);
        this.fuzziTweaks$registerTier3Cargo(base, chest, null, chest, 2);
        this.fuzziTweaks$registerTier3Cargo(base, null, chest, chest, 2);
        this.fuzziTweaks$registerTier3Cargo(base, chest, chest, chest, 3);
    }

    @Unique
    private void fuzziTweaks$registerTier3Cargo(ArrayList<PositionedStack> base, ItemStack s19, ItemStack s20,
        ItemStack s21, int meta) {
        final ArrayList<PositionedStack> input = new ArrayList<>(base);
        if (s19 != null) {
            input.add(new PositionedStack(s19, 90, 0));
        }
        if (s20 != null) {
            input.add(new PositionedStack(s20, 116, 0));
        }
        if (s21 != null) {
            input.add(new PositionedStack(s21, 142, 0));
        }
        new NEIGalacticraftAsteroidsConfig().registerRocketBenchRecipe(
            input,
            new PositionedStack(new ItemStack(AsteroidsItems.tier3Rocket, 1, meta), 139, 102));
    }

    /**
     * Populates the Mars plugin's cargo-bench NEI map with the restored Cargo Rocket recipes.
     * The {@link CargoRocketRecipeHandler} is already registered by GC; GC just never fills
     * this map without the coremod. Positions use GC's own vanilla coordinates (container
     * coords minus the handler's (4,16) offset). The chest in slot 21 sets the capacity:
     * chest metas 3/0/1 -> Cargo Rocket metas 11/12/13.
     */
    @Unique
    private void fuzziTweaks$registerCargoRocket() {
        final ArrayList<PositionedStack> base = new ArrayList<>();
        base.add(new PositionedStack(new ItemStack(GCItems.basicItem, 1, 14), 130, -6)); // advanced wafer
        base.add(new PositionedStack(new ItemStack(GCItems.partNoseCone, 1, 0), 49, 3));
        // Six Heavy-Duty Plates T2 (top three body rows).
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 40, 21));
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 58, 21));
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 40, 39));
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 58, 39));
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 40, 57));
        base.add(new PositionedStack(new ItemStack(MarsItems.marsItemBasic, 1, 3), 58, 57));
        // engine
        base.add(new PositionedStack(new ItemStack(GCItems.rocketEngine, 1, 0), 49, 93));
        // fins
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 22, 75));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 76, 75));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 22, 93));
        base.add(new PositionedStack(new ItemStack(GCItems.partFins, 1, 0), 76, 93));

        this.fuzziTweaks$registerCargoChest(base, 3, 11);
        this.fuzziTweaks$registerCargoChest(base, 0, 12);
        this.fuzziTweaks$registerCargoChest(base, 1, 13);
    }

    @Unique
    private void fuzziTweaks$registerCargoChest(ArrayList<PositionedStack> parts, int chestMeta, int rocketMeta) {
        final ArrayList<PositionedStack> input = new ArrayList<>(parts);
        input.add(new PositionedStack(RecipeUtil.getChestItemStack(1, chestMeta), 130, 30));
        new NEIGalacticraftMarsConfig().registerCargoBenchRecipe(
            input,
            new PositionedStack(new ItemStack(MarsItems.spaceship, 1, rocketMeta), 130, 57));
    }

}

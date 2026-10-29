package com.mrfuzzihead.fuzzitweaks.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.mrfuzzihead.fuzzitweaks.Config;

public enum Mixins implements IMixins {

    MINECRAFT(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("EntityMobMixin", "TileEntityRendererDispatcherMixin")),

    MINECRAFTEXCLHODGEPODGE(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("EntityLivingMixin")
        .addExcludedMod(TargetMods.HODGEPODGE)
        .setApplyIf(() -> Config.enableDespawnModule)),

    AI_IMPROVEMENTS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins(
            "EntityLookHelperMixin",
            "EntityLivingFaceEntityMixin",
            "EntitySquidMixin",
            "EntityAIWatchClosestMixin",
            "EntityAILookIdleMixin",
            "EntityAIAttackOnCollideMixin",
            "EntityWaterMobSpawnMixin",
            "EntityLivingSquidPackMixin")
        .setApplyIf(() -> Config.enableAIImprovementsMain)),

    LEASH_LOAD_FIX(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("EntityLivingLeashMixin", "EntityLeashKnotMixin")
        .setApplyIf(() -> Config.enableLeashLoadFix)),

    SCREENSHOT(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("MinecraftMixin")
        .setApplyIf(() -> Config.enableBackgroundScreenshot)),

    PROJECTE(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("projecte.ItemSearchHelperMixin")
        .addRequiredMod(TargetMods.PROJECTE)
        .setApplyIf(() -> Config.enableProjectETweaks)),

    THAUMICADDITIONS(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("thaumicadditions.KeyBindingsMixin", "thaumicadditions.HUDMixin")
        .addRequiredMod(TargetMods.THAUMICADDITIONS)
        .setApplyIf(() -> Config.enableThaumicAdditionsTweaks)),

    NOTENOUGHITEMS(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("notenoughitems.MixinWorldOverlayRenderer")
        .addRequiredMod(TargetMods.NEI)
        .setApplyIf(() -> Config.enableNEITweaks)),

    MCPATCHER(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("mcpatcher.MobEngineMixin")
        .addRequiredMod(TargetMods.MCPATCHER)
        .setApplyIf(() -> Config.enableMCPatcherTweaks)),

    DISTANTHORIZONS_SERVER(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("distanthorizons.MixinAbstractDhServerLevel")
        .addRequiredMod(TargetMods.DISTANTHORIZONS)
        .setApplyIf(() -> Config.enableDistantHorizonsDimensionFilter)),

    DISTANTHORIZONS_CLIENT(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("distanthorizons.MixinDhClientLevel")
        .addRequiredMod(TargetMods.DISTANTHORIZONS)
        .setApplyIf(() -> Config.enableDistantHorizonsDimensionFilter)),

    GALACTICRAFT(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("galacticraft.TileEntityCoalGeneratorMixin", "galacticraft.SlotSpecificMixin")
        .addClientMixins("galacticraft.NEIGalacticraftConfigMixin")
        .addRequiredMod(TargetMods.GALACTICRAFT)
        .setApplyIf(() -> Config.enableGalacticraftMain)),

    ARTIFICE(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("artifice.ArtificeEnchantsMixin")
        .addRequiredMod(TargetMods.ARTIFICE)
        .setApplyIf(() -> Config.enableArtificeEnchantIdFix)),

    HATS(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("hats.ThreadHatsReaderMixin", "hats.ThreadGetModMobSupportMixin")
        .addRequiredMod(TargetMods.HATS)
        .setApplyIf(() -> Config.enableHatsEndpointFix)),

    TILEENTITY_RESET_FIX(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("forge.ForgeChunkManagerMixin")
        .setApplyIf(() -> Config.enableTileEntityResetFix)),

    // Railcraft is not a coremod, so an early mixin cannot be gated on its mod ID at load time. The mixin
    // itself no-ops when Loader.isModLoaded("Railcraft") is false, and RailcraftCartCompat keeps the
    // Railcraft class reference out of the mixin until that check has passed. The mixin also loads in
    // logging-only mode so the deny can be disabled while the event traces are still collected.
    RAILCRAFT_MINECART_FIX(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("forge.MixinForgeEventFactory")
        .setApplyIf(() -> Config.enableRailcraftMinecartPlacementFix || Config.logMinecartPlacementDebug)),

    CAREFUL_CAST_CORRECTOR(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("BlockChestMixin", "WorldGenDungeonsMixin")
        .setApplyIf(() -> Config.enableCarefulCastCorrector));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}

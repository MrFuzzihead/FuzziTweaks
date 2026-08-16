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

    SCREENSHOT(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("MinecraftMixin")
        .setApplyIf(() -> Config.enableBackgroundScreenshot)),

    PROJECTE(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("projecte.ItemSearchHelperMixin")
        .addRequiredMod(TargetMods.PROJECTE)
        .setApplyIf(() -> Config.enableProjectETweaks)),

    THAUMICADDITIONS(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("thaumicadditions.KeyBindingsMixin")
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

    GALACTICRAFT_COAL_GENERATOR(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("galacticraft.TileEntityCoalGeneratorMixin", "galacticraft.SlotSpecificMixin")
        .addRequiredMod(TargetMods.GALACTICRAFT)
        .setApplyIf(() -> Config.enableGalacticraftCoalGeneratorFuelTweak)),

    GALACTICRAFT_NEI_HANDLERS(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins("galacticraft.NEIGalacticraftConfigMixin")
        .addRequiredMod(TargetMods.GALACTICRAFT)
        .setApplyIf(() -> Config.enableGalacticraftCompressorNEIFix));

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

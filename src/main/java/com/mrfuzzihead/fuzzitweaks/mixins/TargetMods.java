package com.mrfuzzihead.fuzzitweaks.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetMods implements ITargetMod {

    // Read the Javadoc of ITargetMod and TargetModBuilder for further information
    // Add to this enum information about the mods you need to identify during runtime
    PROJECTE("moze_intel.projecte.PECore", "ProjectE"),
    THAUMICADDITIONS("com.pengu.thaumcraft.additions.TA", "thaumicadditions"),
    HODGEPODGE("com.mitchej123.hodgepodge.Hodgepodge", "hodgepodge"),
    NEI("NotEnoughItems"),
    MCPATCHER("mcpatcher"),
    DISTANTHORIZONS("distanthorizons"),
    GALACTICRAFT("micdoodle8.mods.galacticraft.core.GalacticraftCore", "GalacticraftCore");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId);
    }

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}

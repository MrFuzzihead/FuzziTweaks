package com.mrfuzzihead.fuzzitweaks.mixins;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.mrfuzzihead.fuzzitweaks.Config;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        // rename the associated .json file by replacing the "mymodid" with your own mod ID
        // in the .json file edit the "package" and "refmap" properties to match your mod
        // also edit the "refmap" property in the "mixins.mymodid.json" file
        return "mixins.fuzzitweaks.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // Load config here so that setApplyIf conditions read values from the .cfg file
        // rather than the hard-coded field defaults. getMixins() is called before
        // FMLPreInitializationEvent, so we must resolve the config file path ourselves
        // using Launch.minecraftHome which is set by LaunchWrapper very early.
        Config.synchronizeConfiguration(new File(Launch.minecraftHome, "config/fuzzitweaks.cfg"));
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}

package com.mrfuzzihead.fuzzitweaks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cpw.mods.fml.relauncher.FMLInjectionData;

/**
 * Loads real Forge config files through {@link Config} and verifies the values that the README and generated
 * comments promise. These defaults previously drifted from the hard-coded field initializers.
 */
class ConfigTest {

    /**
     * Forge's {@link net.minecraftforge.common.config.Configuration} constructor expects FML to have injected the
     * Minecraft home. Unit tests do not start FML, so provide a harmless non-null home for path formatting.
     */
    @BeforeAll
    static void initializeForgeHome() throws Exception {
        Field minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        if (minecraftHome.get(null) == null) {
            minecraftHome.set(null, new File(".").getAbsoluteFile());
        }
    }

    @Test
    void defaultsMatchTheDocumentedOptions(@TempDir Path tempDir) {
        Config.synchronizeConfiguration(
            tempDir.resolve("defaults.cfg")
                .toFile());

        assertTrue(Config.enableDespawnModule);
        assertTrue(Config.enableBackgroundScreenshot);
        assertTrue(Config.onlyRunLookGoalsNearPlayers);
        assertEquals(0, Config.maxMobBlockLightLevel);
        assertEquals(7, Config.maxMobSkyLightLevel);
        assertArrayEquals(new int[0], Config.distantHorizonsDimensionIds);
        assertEquals("http://dist.creeper.host", Config.hatsDownloadBaseUrl);
    }

    @Test
    void readsOverridesFromASeparateConfigFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("overrides.cfg");
        Files.write(
            file,
            Arrays.asList(
                "minecraft {",
                "    B:EnableDespawnModule=false",
                "    B:EnableBackgroundScreenshot=false",
                "}",
                "ai {",
                "    B:OnlyRunLookGoalsNearPlayers=false",
                "}"),
            StandardCharsets.UTF_8);

        Config.synchronizeConfiguration(file.toFile());

        assertFalse(Config.enableDespawnModule);
        assertFalse(Config.enableBackgroundScreenshot);
        assertFalse(Config.onlyRunLookGoalsNearPlayers);
    }

    @Test
    void doesNotReloadTheSamePathDuringStartup(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("cached.cfg");
        Config.synchronizeConfiguration(file.toFile());
        assertTrue(Config.enableDespawnModule);

        Files.write(file, Arrays.asList("minecraft {", "    B:EnableDespawnModule=false", "}"), StandardCharsets.UTF_8);
        Config.synchronizeConfiguration(file.toFile());

        assertTrue(Config.enableDespawnModule);
    }
}

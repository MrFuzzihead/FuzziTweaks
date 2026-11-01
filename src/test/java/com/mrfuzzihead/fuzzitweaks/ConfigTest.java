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
        assertTrue(Config.enableDoubleDoors);
        assertTrue(Config.enableRightClickClear);
        assertTrue(Config.enableNnbspFix);
        assertTrue(Config.enableSortedEnchantments);
        assertTrue(Config.enableJumpClimbing);
        assertTrue(Config.enableChatLinebreakFix);
        assertTrue(Config.onlyRunLookGoalsNearPlayers);
        assertEquals(Config.ChunkPurgeMode.REPORT, Config.chunkPurgeMode);
        assertEquals(600, Config.chunkPurgeIntervalTicks);
        assertEquals(5, Config.chunkPurgePlayerMargin);
        assertEquals(5, Config.chunkPurgeTicketMargin);
        assertEquals(3, Config.chunkPurgeSpawnMargin);
        assertTrue(Config.chunkPurgeProtectSpawn);
        assertArrayEquals(new int[] { 0 }, Config.chunkPurgeDimensions);
        assertEquals(0, Config.chunkPurgePlayerProtectionRadius);
        assertEquals(0, Config.chunkPurgeMaxPerRun);
        assertEquals(8, Config.chunkPurgeReportSampleSize);
        assertFalse(Config.chunkPurgeProcessEmptyDimensions);
        assertFalse(Config.chunkPurgeDebug);
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
                "}",
                "qol {",
                "    B:EnableDoubleDoors=false",
                "    B:EnableRightClickClear=false",
                "    B:EnableNnbspFix=false",
                "    B:EnableSortedEnchantments=false",
                "    B:EnableJumpClimbing=false",
                "    B:EnableChatLinebreakFix=false",
                "}",
                "chunkpurge {",
                "    S:Mode=PURGE",
                "    I:IntervalTicks=1200",
                "    I:PlayerMargin=0",
                "    I:TicketMargin=1",
                "    I:SpawnMargin=2",
                "    B:ProtectSpawn=false",
                "    I:Dimensions <",
                "        0",
                "        -1",
                "    >",
                "    I:PlayerProtectionRadius=20",
                "    I:MaxPerRun=100",
                "    I:ReportSampleSize=0",
                "    B:ProcessEmptyDimensions=true",
                "    B:Debug=true",
                "}"),
            StandardCharsets.UTF_8);

        Config.synchronizeConfiguration(file.toFile());

        assertFalse(Config.enableDespawnModule);
        assertFalse(Config.enableBackgroundScreenshot);
        assertFalse(Config.enableDoubleDoors);
        assertFalse(Config.enableRightClickClear);
        assertFalse(Config.enableNnbspFix);
        assertFalse(Config.enableSortedEnchantments);
        assertFalse(Config.enableJumpClimbing);
        assertFalse(Config.enableChatLinebreakFix);
        assertFalse(Config.onlyRunLookGoalsNearPlayers);
        assertEquals(Config.ChunkPurgeMode.PURGE, Config.chunkPurgeMode);
        assertEquals(1200, Config.chunkPurgeIntervalTicks);
        assertEquals(0, Config.chunkPurgePlayerMargin);
        assertEquals(1, Config.chunkPurgeTicketMargin);
        assertEquals(2, Config.chunkPurgeSpawnMargin);
        assertFalse(Config.chunkPurgeProtectSpawn);
        assertArrayEquals(new int[] { 0, -1 }, Config.chunkPurgeDimensions);
        assertEquals(20, Config.chunkPurgePlayerProtectionRadius);
        assertEquals(100, Config.chunkPurgeMaxPerRun);
        assertEquals(0, Config.chunkPurgeReportSampleSize);
        assertTrue(Config.chunkPurgeProcessEmptyDimensions);
        assertTrue(Config.chunkPurgeDebug);
    }

    @Test
    void invalidChunkPurgeModeFallsBackToReport(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("bad-mode.cfg");
        Files.write(file, Arrays.asList("chunkpurge {", "    S:Mode=NOT_A_MODE", "}"), StandardCharsets.UTF_8);

        Config.synchronizeConfiguration(file.toFile());

        assertEquals(Config.ChunkPurgeMode.REPORT, Config.chunkPurgeMode);
    }

    @Test
    void reloadRereadsTheCurrentConfigFile(@TempDir Path tempDir) throws IOException {
        final Path file = tempDir.resolve("reload.cfg");
        Files.write(file, Arrays.asList("chunkpurge {", "    S:Mode=REPORT", "}"), StandardCharsets.UTF_8);
        Config.synchronizeConfiguration(file.toFile());
        assertEquals(Config.ChunkPurgeMode.REPORT, Config.chunkPurgeMode);

        Files.write(file, Arrays.asList("chunkpurge {", "    S:Mode=PURGE", "}"), StandardCharsets.UTF_8);
        Config.synchronizeConfiguration(file.toFile());
        assertEquals(Config.ChunkPurgeMode.REPORT, Config.chunkPurgeMode);

        Config.reload();
        assertEquals(Config.ChunkPurgeMode.PURGE, Config.chunkPurgeMode);
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

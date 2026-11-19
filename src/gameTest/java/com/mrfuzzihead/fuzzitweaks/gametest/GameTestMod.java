package com.mrfuzzihead.fuzzitweaks.gametest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

/**
 * Test-only mod: a separate, non-shipped {@code @Mod} that asserts FuzziTweaks works against the real game.
 *
 * <p>
 * It lives in the {@code gameTest} source set and is only put on the classpath by {@code ./gradlew gameTestServer}
 * (or by any run task with {@code -PgameTest}), so it never reaches the published jar. Run it with:
 * </p>
 *
 * <pre>
 * mkdir -p run/server &amp;&amp; echo "eula=true" &gt; run/server/eula.txt
 * ./gradlew gameTestServer
 * </pre>
 *
 * <p>
 * The assertions run on the server thread at {@link FMLServerStartedEvent}, where the overworld exists and block and
 * light mutations are legal without a tick-loop hand-off. They cover the parts of FuzziTweaks that only exist in a
 * live game: the mixin-transformed spawn-light rules, the entity-join AI stripping and the double-door handler.
 * Everything that can be tested headless is in {@code src/test/java} instead.
 * </p>
 */
@Mod(
    modid = GameTestMod.MOD_ID,
    name = "FuzziTweaks Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:fuzzitweaks")
public class GameTestMod {

    public static final String MOD_ID = "fuzzitweaksgametest";

    static final String OK_MARKER = "FUZZITWEAKS_GAMETEST_OK";
    static final String FAIL_MARKER = "FUZZITWEAKS_GAMETEST_FAILED";

    /** Set by the build for game test runs; without it the class does nothing even if it is on the classpath. */
    private static final String ENABLE_PROPERTY = "fuzzitweaks.gameTest";

    /** Guards against a second {@link FMLServerStartedEvent}, for example after a world reload. */
    private boolean ran;

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (this.ran) {
            return;
        }
        this.ran = true;

        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            System.out.println("[FUZZITWEAKS-GAMETEST] " + ENABLE_PROPERTY + " is not set, not running the tests");
            return;
        }

        int failures;
        int checks;
        try {
            failures = ServerGameTests.runAll();
            checks = ServerGameTests.checkCount();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(FAIL_MARKER + ": " + t);
            exit(1);
            return;
        }

        System.out.println("[FUZZITWEAKS-GAMETEST] " + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            System.out.println(FAIL_MARKER + ": " + failures + " assertion(s) failed, see above");
            exit(1);
        } else {
            System.out.println(OK_MARKER);
            exit(0);
        }
    }

    /**
     * Stops the run with the test's exit status. {@code halt=false} lets the game run its shutdown hooks, which is
     * what flushes the JaCoCo agent's {@code gameTest.exec}.
     */
    private static void exit(int status) {
        FMLCommonHandler.instance()
            .exitJava(status, false);
    }
}

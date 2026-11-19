package com.mrfuzzihead.fuzzitweaks.gametest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

import net.minecraft.block.BlockDoor;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.mojang.authlib.GameProfile;
import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * The in-game assertions. Each check logs {@code [FUZZITWEAKS-GAMETEST] ok}/{@code FAIL} with its name, so the exit
 * status and the log agree.
 *
 * <p>
 * Everything here needs the real game: a live {@link WorldServer}, registered blocks, the Forge event bus and the
 * mixin-transformed Minecraft classes. A failure of a check never aborts the others - the remaining checks still run
 * and report, so one broken feature does not hide the state of the rest.
 * </p>
 */
public final class ServerGameTests {

    private static final String FAIL = "[FUZZITWEAKS-GAMETEST] FAIL ";
    private static final String PASS = "[FUZZITWEAKS-GAMETEST] ok   ";

    /** A spot near spawn that is inside the loaded spawn chunks. */
    private static final int BASE_X = 8;
    private static final int BASE_Z = 8;
    private static final int BASE_Y = 100;

    /** {@code EntityMob#isValidLightLevel} is protected; 1.7.10's SRG name is covered as a production fallback. */
    private static Method isValidLightLevel;

    private static int failures;
    private static int checks;

    private ServerGameTests() {}

    /**
     * Runs every assertion.
     *
     * @return the number of failed assertions
     */
    public static int runAll() {
        failures = 0;
        checks = 0;

        final MinecraftServer server = MinecraftServer.getServer();
        final WorldServer world = server == null ? null : server.worldServerForDimension(0);
        if (world == null) {
            failures++;
            checks++;
            System.out.println(FAIL + "the overworld is not loaded");
            return failures;
        }

        check("the config file is written and the light limits are in range", () -> checkConfiguration());
        check("EntityMobMixin rejects every block light above MaxMobSpawnLightLevel", () -> checkBlockLightRule(world));
        check("EntityMobMixin uses MaxMobSkyLightLevel as the random spawn threshold", () -> checkSkyLightRule(world));
        check("AIGoalStripper matches the configured look-goal options", () -> checkLookGoals(world));
        check("DoubleDoors opens the other leaf from a real interact event", () -> checkDoubleDoors(world));

        return failures;
    }

    /**
     * @return how many assertions {@link #runAll()} executed, for the combined summary
     */
    public static int checkCount() {
        return checks;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Checks
    // ---------------------------------------------------------------------------------------------------------

    /**
     * The pre-init config write and the ranges the config parser enforces. A dedicated server has no client config,
     * so this also proves the server-side defaults are usable.
     */
    private static void checkConfiguration() {
        final File file = Config.resolveConfigurationFile(null);
        require(file.isFile(), "the config file was not written: " + file);
        requireInRange("maxMobBlockLightLevel", Config.maxMobBlockLightLevel);
        requireInRange("maxMobSkyLightLevel", Config.maxMobSkyLightLevel);
    }

    /**
     * The block-light half of {@code EntityMobMixin}: a spawn is rejected as soon as the saved block light exceeds
     * {@link Config#maxMobBlockLightLevel}, which vanilla only does at random (one chance in eight per call at light
     * 1), so a loop over enough attempts cannot pass by accident. The config is written at runtime on purpose: the
     * mixin reads it per call, and this proves it is not baked in at class load.
     */
    private static void checkBlockLightRule(WorldServer world) throws Exception {
        final int savedBlockLight = Config.maxMobBlockLightLevel;
        final int savedSkyLight = Config.maxMobSkyLightLevel;
        try {
            Config.maxMobBlockLightLevel = 0;
            Config.maxMobSkyLightLevel = 7;
            final EntityZombie zombie = placeMob(world, BASE_X, BASE_Z, 1);
            final Method method = isValidLightLevel();

            for (int attempt = 0; attempt < 100; attempt++) {
                if (Boolean.TRUE.equals(method.invoke(zombie))) {
                    throw new AssertionError("block light 1 was accepted on attempt " + attempt);
                }
            }
        } finally {
            Config.maxMobBlockLightLevel = savedBlockLight;
            Config.maxMobSkyLightLevel = savedSkyLight;
        }
    }

    /**
     * The sky-light half: {@code EntityMobMixin} replaces the {@code Random#nextInt(8)} bound of the final check with
     * {@link Config#maxMobSkyLightLevel} + 1. A bound of 1 can only roll 0, so a block at light 3 must be rejected
     * every time, while vanilla's bound of 8 would accept it five times out of eight.
     */
    private static void checkSkyLightRule(WorldServer world) throws Exception {
        final int savedBlockLight = Config.maxMobBlockLightLevel;
        final int savedSkyLight = Config.maxMobSkyLightLevel;
        try {
            Config.maxMobBlockLightLevel = 15;
            Config.maxMobSkyLightLevel = 0;
            final EntityZombie zombie = placeMob(world, BASE_X, BASE_Z, 3);
            final Method method = isValidLightLevel();

            for (int attempt = 0; attempt < 100; attempt++) {
                if (Boolean.TRUE.equals(method.invoke(zombie))) {
                    throw new AssertionError("sky-light bound 1 accepted light 3 on attempt " + attempt);
                }
            }
        } finally {
            Config.maxMobBlockLightLevel = savedBlockLight;
            Config.maxMobSkyLightLevel = savedSkyLight;
        }
    }

    /**
     * {@code AIGoalStripper} resolves its options once at pre-init, so this asserts whichever mode the run was
     * configured for: with the options on a joining zombie must lose both goals, with them off it must keep them.
     */
    private static void checkLookGoals(WorldServer world) {
        final EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(BASE_X + 0.5D, BASE_Y, BASE_Z + 0.5D);
        world.spawnEntityInWorld(zombie);
        try {
            final boolean stripWatchClosest = Config.enableAIImprovementsMain && Config.removeLookAtPlayerGoal;
            final boolean stripLookIdle = Config.enableAIImprovementsMain && Config.removeLookIdleGoal;
            final boolean hasWatchClosest = hasTask(zombie, "EntityAIWatchClosest");
            final boolean hasLookIdle = hasTask(zombie, "EntityAILookIdle");

            require(
                hasWatchClosest != stripWatchClosest,
                "EntityAIWatchClosest is " + (hasWatchClosest ? "present" : "absent")
                    + " but RemoveLookAtPlayerGoal is "
                    + Config.removeLookAtPlayerGoal);
            require(
                hasLookIdle != stripLookIdle,
                "EntityAILookIdle is " + (hasLookIdle ? "present" : "absent")
                    + " but RemoveLookIdleGoal is "
                    + Config.removeLookIdleGoal);
        } finally {
            zombie.setDead();
        }
    }

    /**
     * Places a real double door and drives the interaction the way a server does: post the {@link PlayerInteractEvent}
     * (which is where {@code DoubleDoors} opens the other leaf through its own synthetic event) and then activate the
     * clicked leaf as vanilla would. Both halves must end up open.
     */
    private static void checkDoubleDoors(WorldServer world) {
        final int x = BASE_X + 4;
        final int z = BASE_Z;
        final int y = BASE_Y;
        clearArea(world, x, y, z);
        // Facing 0 makes the leaves neighbors along +Z (see DoubleDoors#partnerOffset); the hinge lives in bit 0 of
        // the upper half, and the two leaves need opposite hinges to count as a pair.
        setDoor(world, x, y, z, 0, 0);
        setDoor(world, x, y, z + 1, 0, 1);

        final EntityPlayer player = new TestPlayer(world, x, y, z);

        final BlockDoor door = (BlockDoor) Blocks.wooden_door;
        try {
            require(!door.func_150015_f(world, x, y, z), "the fresh door pair is already open");

            MinecraftForge.EVENT_BUS
                .post(new PlayerInteractEvent(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, 1, world));
            require(
                door.onBlockActivated(world, x, y, z, player, 1, 0.5F, 0.5F, 0.5F),
                "the clicked leaf rejected the interaction");

            require(door.func_150015_f(world, x, y, z), "the clicked leaf did not open");
            require(door.func_150015_f(world, x, y, z + 1), "the other leaf did not open");
        } finally {
            clearArea(world, x, y, z);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    private interface Check {

        void run() throws Exception;
    }

    private static void check(String name, Check check) {
        checks++;
        try {
            check.run();
            System.out.println(PASS + name);
        } catch (Throwable t) {
            failures++;
            System.out.println(FAIL + name + ": " + t);
            t.printStackTrace(System.out);
        }
    }

    /**
     * Spawns a zombie at a fixed spot and forces the block and sky light the spawn rule reads.
     *
     * @param blockLight the saved block light to force
     * @return the zombie, positioned so that {@code floor(boundingBox.minY)} is the lit block
     */
    private static EntityZombie placeMob(WorldServer world, int x, int z, int blockLight) {
        clearArea(world, x, BASE_Y, z);
        final EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(x + 0.5D, BASE_Y, z + 0.5D);
        final int y = MathHelper.floor_double(zombie.boundingBox.minY);
        world.setLightValue(EnumSkyBlock.Sky, x, y, z, 0);
        world.setLightValue(EnumSkyBlock.Block, x, y, z, blockLight);
        return zombie;
    }

    /** Clears a two-block-high column and its four neighbors' doors, with no neighbor updates. */
    private static void clearArea(WorldServer world, int x, int y, int z) {
        for (int dy = 0; dy < 2; dy++) {
            world.setBlock(x, y + dy, z, Blocks.air, 0, 2);
            world.setBlock(x, y + dy, z + 1, Blocks.air, 0, 2);
        }
        world.setLightValue(EnumSkyBlock.Block, x, y, z, 0);
    }

    /** Places one leaf: metadata 0/8 are the lower/upper half, and {@code hinge} is the upper half's bit 0. */
    private static void setDoor(WorldServer world, int x, int y, int z, int facing, int hinge) {
        world.setBlock(x, y, z, Blocks.wooden_door, facing, 2);
        world.setBlock(x, y + 1, z, Blocks.wooden_door, 8 | hinge, 2);
    }

    private static boolean hasTask(EntityLiving entity, String actionClassName) {
        for (EntityAITasks.EntityAITaskEntry entry : entity.tasks.taskEntries) {
            if (entry.action != null && actionClassName.equals(
                entry.action.getClass()
                    .getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    private static Method isValidLightLevel() throws NoSuchMethodException {
        if (isValidLightLevel == null) {
            Method method = null;
            for (Method candidate : EntityMob.class.getDeclaredMethods()) {
                if ("isValidLightLevel".equals(candidate.getName()) || "func_70814_o".equals(candidate.getName())) {
                    method = candidate;
                    break;
                }
            }
            if (method == null) {
                throw new NoSuchMethodException("EntityMob has no isValidLightLevel");
            }
            method.setAccessible(true);
            isValidLightLevel = method;
        }
        return isValidLightLevel;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireInRange(String name, int value) {
        require(value >= 0 && value <= 15, name + " is out of range: " + value);
    }

    /**
     * A player that is not backed by a connection. {@code DoubleDoors} only asks whether the player is sneaking, but
     * {@link PlayerInteractEvent} needs a real {@link EntityPlayer}, and {@link EntityPlayerMP} would drag in a
     * network handler and the player list.
     */
    private static final class TestPlayer extends EntityPlayer {

        private final ChunkCoordinates position;

        private TestPlayer(WorldServer world, int x, int y, int z) {
            super(world, new GameProfile(UUID.randomUUID(), "FuzziTweaksTest"));
            this.position = new ChunkCoordinates(x, y, z);
        }

        @Override
        public String getCommandSenderName() {
            return "FuzziTweaksTest";
        }

        @Override
        @SuppressWarnings("unused")
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent message) {}

        @Override
        public boolean canCommandSenderUseCommand(int permissionLevel, String command) {
            return false;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return this.position;
        }
    }
}

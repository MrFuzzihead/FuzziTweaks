package com.mrfuzzihead.fuzzitweaks.commands;

import java.util.Locale;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.chunkpurge.ChunkPurge;

/**
 * Subcommands for {@code /fuzzitweaks chunkpurge}. Kept in the mod namespace so it cannot clash with a separately
 * installed ChunkPurge mod's {@code /chunkpurge}.
 */
public final class ChunkPurgeCommand {

    private ChunkPurgeCommand() {}

    public static void handle(ICommandSender sender, String[] args) {
        // args[0] is "chunkpurge".
        if (args.length == 1) {
            status(sender);
            return;
        }

        final String subcommand = args[1].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status":
                status(sender);
                return;
            case "report":
                scan(sender, args, Config.ChunkPurgeMode.REPORT);
                return;
            case "run":
                scan(sender, args, Config.chunkPurgeMode);
                return;
            case "reload":
                reload(sender);
                return;
            default:
                help(sender);
        }
    }

    private static void scan(ICommandSender sender, String[] args, Config.ChunkPurgeMode mode) {
        if (args.length > 3) {
            send(sender, EnumChatFormatting.RED + "Usage: /fuzzitweaks chunkpurge " + args[1] + " [dimension]");
            return;
        }
        if (mode == Config.ChunkPurgeMode.OFF) {
            send(
                sender,
                EnumChatFormatting.RED
                    + "Chunk purge is OFF. Set Mode=REPORT or Mode=PURGE in the chunkpurge config section, then reload.");
            return;
        }

        Integer requestedDimension = null;
        if (args.length == 3) {
            try {
                requestedDimension = Integer.valueOf(args[2]);
            } catch (NumberFormatException e) {
                send(sender, EnumChatFormatting.RED + "Dimension must be an integer.");
                return;
            }
        }

        final MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            send(sender, EnumChatFormatting.RED + "Server is not available.");
            return;
        }

        if (requestedDimension != null) {
            final WorldServer world = server.worldServerForDimension(requestedDimension);
            if (world == null) {
                send(sender, EnumChatFormatting.RED + "Dimension " + requestedDimension + " is not loaded.");
                return;
            }
            print(sender, ChunkPurge.scan(world, mode, false));
            return;
        }

        final int[] dimensions = Config.chunkPurgeDimensions;
        if (dimensions == null || dimensions.length == 0) {
            send(sender, EnumChatFormatting.RED + "No dimensions are configured.");
            return;
        }

        for (int dimension : dimensions) {
            final WorldServer world = server.worldServerForDimension(dimension);
            if (world == null) {
                send(sender, EnumChatFormatting.GRAY + "Dimension " + dimension + " is not loaded.");
                continue;
            }
            print(sender, ChunkPurge.scan(world, mode, true));
        }
    }

    private static void status(ICommandSender sender) {
        send(
            sender,
            EnumChatFormatting.GREEN + "Chunk purge: mode="
                + Config.chunkPurgeMode
                + (Config.chunkPurgeMode == Config.ChunkPurgeMode.REPORT ? " (diagnostic only)" : ""));
        send(
            sender,
            EnumChatFormatting.GRAY + "  interval="
                + Config.chunkPurgeIntervalTicks
                + " ticks, dimensions="
                + dimensionsAsString()
                + ", playerRadius="
                + ChunkPurge.getPlayerProtectionRadius()
                + ", ticketMargin="
                + Config.chunkPurgeTicketMargin
                + ", spawnMargin="
                + Config.chunkPurgeSpawnMargin
                + ", maxPerRun="
                + Config.chunkPurgeMaxPerRun);
        send(
            sender,
            EnumChatFormatting.GRAY + "  protectSpawn="
                + Config.chunkPurgeProtectSpawn
                + ", processEmptyDimensions="
                + Config.chunkPurgeProcessEmptyDimensions
                + ", sampleSize="
                + Config.chunkPurgeReportSampleSize
                + ", debug="
                + Config.chunkPurgeDebug);

        final int[] dimensions = Config.chunkPurgeDimensions;
        if (dimensions == null || dimensions.length == 0) {
            send(sender, EnumChatFormatting.RED + "  No dimensions configured.");
            return;
        }

        for (int dimension : dimensions) {
            final ChunkPurge.RunStats stats = ChunkPurge.getLastRun(dimension);
            if (stats == null) {
                send(sender, EnumChatFormatting.GRAY + "  dim " + dimension + ": no scan yet");
            } else if (stats.skipReason != null) {
                send(sender, EnumChatFormatting.GRAY + "  dim " + dimension + ": skipped (" + stats.skipReason + ")");
            } else {
                send(
                    sender,
                    EnumChatFormatting.GRAY + "  dim "
                        + dimension
                        + ": loaded="
                        + stats.loaded
                        + " protected="
                        + stats.protectedChunks
                        + " unloadable="
                        + stats.unloadable
                        + " queued="
                        + stats.queued
                        + " lastTick="
                        + stats.tick);
            }
        }
    }

    private static void reload(ICommandSender sender) {
        Config.reload();
        send(
            sender,
            EnumChatFormatting.GREEN + "Chunk purge config reloaded: mode="
                + Config.chunkPurgeMode
                + ", interval="
                + Config.chunkPurgeIntervalTicks
                + ", dimensions="
                + dimensionsAsString());
    }

    private static void print(ICommandSender sender, ChunkPurge.RunStats stats) {
        if (stats.skipReason != null) {
            send(sender, EnumChatFormatting.RED + "ChunkPurge [" + stats.dimension + "] skipped: " + stats.skipReason);
            return;
        }

        final EnumChatFormatting color = stats.mode == Config.ChunkPurgeMode.PURGE ? EnumChatFormatting.YELLOW
            : EnumChatFormatting.GREEN;
        send(
            sender,
            color + "ChunkPurge "
                + stats.mode
                + " ["
                + stats.dimension
                + "]: "
                + stats.unloadable
                + " unloadable / "
                + stats.loaded
                + " loaded ("
                + stats.protectedChunks
                + " protected), queued="
                + stats.queued
                + ", "
                + stats.elapsedMillis()
                + " ms");
    }

    private static void help(ICommandSender sender) {
        send(sender, EnumChatFormatting.GREEN + "Chunk purge commands:");
        send(sender, EnumChatFormatting.GRAY + "/fuzzitweaks chunkpurge status");
        send(sender, EnumChatFormatting.GRAY + "/fuzzitweaks chunkpurge report [dimension] - read-only scan");
        send(sender, EnumChatFormatting.GRAY + "/fuzzitweaks chunkpurge run [dimension] - uses configured mode");
        send(sender, EnumChatFormatting.GRAY + "/fuzzitweaks chunkpurge reload");
    }

    private static String dimensionsAsString() {
        final int[] dimensions = Config.chunkPurgeDimensions;
        if (dimensions == null || dimensions.length == 0) {
            return "[]";
        }

        final StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < dimensions.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(dimensions[i]);
        }
        return builder.append(']')
            .toString();
    }

    private static void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }
}

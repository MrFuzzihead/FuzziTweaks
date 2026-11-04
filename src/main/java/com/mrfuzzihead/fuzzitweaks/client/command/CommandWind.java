package com.mrfuzzihead.fuzzitweaks.client.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.client.wind.WindRuntime;

/**
 * Client-side {@code /fuzzitweaks wind} command.
 *
 * <p>
 * Wind is entirely client-side, so this is registered as a Forge client command rather than a server command:
 * it works in singleplayer and on any server, even one that does not run FuzziTweaks. The command only reports
 * state; it does not change anything, so it cannot desync a server.
 *
 * <p>
 * Subcommands other than {@code wind} are forwarded to the server unchanged, so a server-side
 * {@code /fuzzitweaks} command (chunk purge diagnostics, for example) still works when both sides run the mod.
 */
public final class CommandWind extends CommandBase {

    @Override
    public String getCommandName() {
        return "fuzzitweaks";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fuzzitweaks wind";
    }

    /**
     * Client commands still go through the normal permission check, and the client player reports permission
     * level 0. A level-4 command would be rejected even in singleplayer.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || !"wind".equalsIgnoreCase(args[0])) {
            if (sender instanceof EntityClientPlayerMP) {
                final StringBuilder command = new StringBuilder("/fuzzitweaks");
                for (String arg : args) {
                    command.append(' ')
                        .append(arg);
                }
                ((EntityClientPlayerMP) sender).sendChatMessage(command.toString());
            } else {
                send(sender, EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender));
            }
            return;
        }
        if (args.length != 1) {
            send(sender, EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender));
            return;
        }

        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            send(sender, EnumChatFormatting.RED + "No world loaded.");
            return;
        }

        final WindRuntime.DebugInfo info = WindRuntime
            .debug(minecraft.theWorld, minecraft.thePlayer.posX, minecraft.thePlayer.posZ);

        final String filterKind = info.denylist ? "denylist" : "allowlist";
        final String filter = filterKind + " " + Arrays.toString(info.dimensionIds);

        if (!info.masterEnabled) {
            send(sender, EnumChatFormatting.AQUA + "FuzziTweaks wind: " + EnumChatFormatting.RED + "disabled");
            send(sender, EnumChatFormatting.GRAY + "  EnableWindParticles=false in the wind config section.");
            return;
        }
        if (!info.enabledInDimension) {
            send(
                sender,
                EnumChatFormatting.AQUA + "FuzziTweaks wind: "
                    + EnumChatFormatting.RED
                    + "disabled in dimension "
                    + info.dimensionId);
            send(sender, EnumChatFormatting.GRAY + "  Dimension filter: " + filter + ".");
            return;
        }

        send(
            sender,
            EnumChatFormatting.AQUA + "FuzziTweaks wind: "
                + EnumChatFormatting.GREEN
                + "enabled in dimension "
                + info.dimensionId);
        send(sender, EnumChatFormatting.GRAY + "  Dimension filter: " + filter + ".");
        send(
            sender,
            EnumChatFormatting.GRAY + "  World time "
                + info.worldTime
                + " ("
                + format(info.worldTime / 24_000.0D)
                + " days) | wind clock "
                + info.windTime);
        send(
            sender,
            EnumChatFormatting.GRAY + "  Base "
                + format(info.baseSpeedBps)
                + " b/s at "
                + format(info.baseDirectionDegrees)
                + " deg | gust x"
                + format(info.gust));
        send(
            sender,
            EnumChatFormatting.GRAY + "  Here "
                + format(info.localSpeedBps)
                + " b/s at "
                + format(info.localDirectionDegrees)
                + " deg | vertical "
                + signed(info.verticalBps)
                + " b/s");
        send(
            sender,
            EnumChatFormatting.GRAY + "  Config: max "
                + format(Config.windMaxSpeed)
                + " b/s, turbulence "
                + format(Config.windTurbulence)
                + " b/s @ "
                + format(Config.windTurbulenceScale)
                + " blocks, response "
                + format(Config.windParticleResponse)
                + ", vertical "
                + format(Config.windVerticalStrength)
                + " b/s");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "wind") : null;
    }

    private static void send(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText(line));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }
}

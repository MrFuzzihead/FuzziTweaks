package com.mrfuzzihead.fuzzitweaks.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandFuzziTweaks extends CommandBase {

    @Override
    public String getCommandName() {
        return "fuzzitweaks";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fuzzitweaks chunkpurge <status|report|run|reload> [dimension]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "chunkpurge".equalsIgnoreCase(args[0])) {
            ChunkPurgeCommand.handle(sender, args);
            return;
        }

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "FuzziTweaks commands:"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + getCommandUsage(sender)));
    }
}

package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

/**
 * Covers the pure component-tree split used by the chat line-break fix. Rendering, chat-line ids and the interaction
 * with other chat mods still need an in-game check.
 */
class ChatLineSplitterTest {

    @Test
    void returnsTheOriginalComponentWhenThereIsNoNewline() {
        final ChatComponentText component = new ChatComponentText("single line");
        assertSame(
            component,
            ChatLineSplitter.split(component)
                .get(0));
    }

    @Test
    void splitsPlainTextOnNewlines() {
        final List<IChatComponent> lines = ChatLineSplitter.split(new ChatComponentText("first\nsecond"));
        assertEquals(2, lines.size());
        assertEquals(
            "first",
            lines.get(0)
                .getUnformattedText());
        assertEquals(
            "second",
            lines.get(1)
                .getUnformattedText());
    }

    @Test
    void keepsEmptyAndTrailingLines() {
        final List<IChatComponent> lines = ChatLineSplitter.split(new ChatComponentText("a\n\nb\n"));
        assertEquals(4, lines.size());
        assertEquals(
            "a",
            lines.get(0)
                .getUnformattedText());
        assertEquals(
            "",
            lines.get(1)
                .getUnformattedText());
        assertEquals(
            "b",
            lines.get(2)
                .getUnformattedText());
        assertEquals(
            "",
            lines.get(3)
                .getUnformattedText());
    }

    @Test
    void stripsTheCarriageReturnOfCrLf() {
        final List<IChatComponent> lines = ChatLineSplitter.split(new ChatComponentText("first\r\nsecond"));
        assertEquals(2, lines.size());
        assertEquals(
            "first",
            lines.get(0)
                .getUnformattedText());
        assertEquals(
            "second",
            lines.get(1)
                .getUnformattedText());
    }

    @Test
    void copiesTheComponentStyleAndItsClickEventOntoEveryLine() {
        final ChatStyle style = new ChatStyle().setColor(EnumChatFormatting.RED)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/test"));
        final ChatComponentText component = new ChatComponentText("first\nsecond");
        component.setChatStyle(style);

        final List<IChatComponent> lines = ChatLineSplitter.split(component);
        assertEquals(2, lines.size());
        assertNotSame(
            lines.get(0)
                .getChatStyle(),
            lines.get(1)
                .getChatStyle());
        for (IChatComponent line : lines) {
            assertEquals(
                EnumChatFormatting.RED,
                line.getChatStyle()
                    .getColor());
            assertEquals(
                "/test",
                line.getChatStyle()
                    .getChatClickEvent()
                    .getValue());
        }
    }

    @Test
    void keepsSiblingStylesIndependent() {
        final ChatComponentText root = new ChatComponentText("first");
        root.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED));
        final ChatComponentText sibling = new ChatComponentText("\nsecond");
        sibling.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));
        root.appendSibling(sibling);

        final List<IChatComponent> lines = ChatLineSplitter.split(root);
        assertEquals(2, lines.size());
        assertEquals(
            "first",
            lines.get(0)
                .getUnformattedText());
        assertEquals(
            EnumChatFormatting.RED,
            lines.get(0)
                .getChatStyle()
                .getColor());
        assertEquals(
            "second",
            lines.get(1)
                .getUnformattedText());
        assertEquals(
            EnumChatFormatting.GREEN,
            lines.get(1)
                .getChatStyle()
                .getColor());
    }

    @Test
    void carriesRawFormattingOverALineBreak() {
        assertEquals("§cnext", secondLine("§cred\nnext"));
        assertEquals("§c§lnext", secondLine("§c§lred\nnext"));
        assertEquals("next", secondLine("§cred§r\nnext"));
        assertEquals("§lnext", secondLine("§lred\nnext"));
    }

    private static String secondLine(String text) {
        return ChatLineSplitter.split(new ChatComponentText(text))
            .get(1)
            .getUnformattedText();
    }
}

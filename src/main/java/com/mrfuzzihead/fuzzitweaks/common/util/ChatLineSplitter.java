package com.mrfuzzihead.fuzzitweaks.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

/**
 * Splits a chat component on embedded newlines into one component per line.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock), but instead of splitting {@code getFormattedText()} and rebuilding plain
 * {@code ChatComponentText}s (which drops click/hover events and can lose formatting at a line boundary), this walks
 * the component tree with {@link IChatComponent#iterator()} and carries each leaf's effective {@link ChatStyle} onto
 * the new line. The style deep-copy contains the inherited colours/styles and the click/hover events, so a tooltip
 * link or a coloured line keeps working after the split.
 *
 * <p>
 * Older mods often put raw {@code §} codes inside a {@code ChatComponentText} instead of using styles. A colour or
 * style code only applies to the rest of that string, so a plain split would lose it at the newline. The splitter
 * therefore tracks the active raw formatting inside each leaf's text and prefixes the next line with it, while
 * treating a new leaf as a fresh formatting scope (vanilla resets after every leaf anyway).
 *
 * <p>
 * The returned list is a singleton containing the original component when there is no newline, so the normal
 * (single-line) chat path allocates nothing. Callers must not mutate the returned components.
 */
public final class ChatLineSplitter {

    private static final char SECTION_SIGN = '\u00a7';

    private ChatLineSplitter() {}

    public static List<IChatComponent> split(IChatComponent component) {
        if (component == null || component.getUnformattedText()
            .indexOf('\n') < 0) {
            return Collections.singletonList(component);
        }

        final List<Segment> segments = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final Iterator<IChatComponent> iterator = component.iterator();
        while (iterator.hasNext()) {
            appendLeaf(iterator.next(), segments);
        }

        final List<IChatComponent> lines = new ArrayList<>();
        List<Segment> line = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.newline) {
                lines.add(buildLine(line));
                line = new ArrayList<>();
            } else {
                line.add(segment);
            }
        }
        if (!line.isEmpty()) {
            lines.add(buildLine(line));
        }
        if (lines.isEmpty()) {
            lines.add(new ChatComponentText(""));
        }
        return lines;
    }

    private static void appendLeaf(IChatComponent leaf, List<Segment> segments) {
        final String text = leaf.getUnformattedTextForChat();
        final ChatStyle style = leaf.getChatStyle()
            .createDeepCopy();
        final String[] parts = text.split("\n", -1);
        final RawFormatting formatting = new RawFormatting();
        String carry = "";

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("\r")) {
                part = part.substring(0, part.length() - 1);
            }

            segments.add(new Segment(carry + part, style.createShallowCopy()));
            if (i < parts.length - 1) {
                formatting.scan(part);
                carry = formatting.prefix();
                segments.add(Segment.NEWLINE);
            }
        }
    }

    private static IChatComponent buildLine(List<Segment> segments) {
        if (segments.isEmpty()) {
            return new ChatComponentText("");
        }

        final Segment first = segments.get(0);
        final ChatComponentText line = new ChatComponentText(first.text);
        line.setChatStyle(first.style);
        for (int i = 1; i < segments.size(); i++) {
            final Segment segment = segments.get(i);
            final ChatComponentText piece = new ChatComponentText(segment.text);
            piece.setChatStyle(segment.style);
            // Add directly instead of appendSibling: appendSibling would make this piece inherit the first
            // segment's style, colouring the rest of the line when the first segment is coloured.
            line.getSiblings()
                .add(piece);
        }
        return line;
    }

    private static final class Segment {

        static final Segment NEWLINE = new Segment("", null, true);

        private final String text;
        private final ChatStyle style;
        private final boolean newline;

        Segment(String text, ChatStyle style) {
            this(text, style, false);
        }

        Segment(String text, ChatStyle style, boolean newline) {
            this.text = text;
            this.style = style;
            this.newline = newline;
        }
    }

    /**
     * Tracks the raw {@code §} formatting active at the end of a text leaf. Colour codes reset the style flags, as
     * they do in vanilla's font renderer.
     */
    private static final class RawFormatting {

        private char color;
        private boolean obfuscated;
        private boolean bold;
        private boolean strikethrough;
        private boolean underlined;
        private boolean italic;

        void scan(String text) {
            for (int i = 0; i + 1 < text.length(); i++) {
                if (text.charAt(i) != SECTION_SIGN) {
                    continue;
                }

                final char code = Character.toLowerCase(text.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    this.color = code;
                    this.obfuscated = false;
                    this.bold = false;
                    this.strikethrough = false;
                    this.underlined = false;
                    this.italic = false;
                } else if (code == 'k') {
                    this.obfuscated = true;
                } else if (code == 'l') {
                    this.bold = true;
                } else if (code == 'm') {
                    this.strikethrough = true;
                } else if (code == 'n') {
                    this.underlined = true;
                } else if (code == 'o') {
                    this.italic = true;
                } else if (code == 'r') {
                    this.reset();
                }

                i++;
            }
        }

        String prefix() {
            if (this.color == 0 && !this.obfuscated
                && !this.bold
                && !this.strikethrough
                && !this.underlined
                && !this.italic) {
                return "";
            }

            final StringBuilder builder = new StringBuilder();
            if (this.color != 0) {
                builder.append(SECTION_SIGN)
                    .append(this.color);
            }
            if (this.obfuscated) {
                builder.append(SECTION_SIGN)
                    .append('k');
            }
            if (this.bold) {
                builder.append(SECTION_SIGN)
                    .append('l');
            }
            if (this.strikethrough) {
                builder.append(SECTION_SIGN)
                    .append('m');
            }
            if (this.underlined) {
                builder.append(SECTION_SIGN)
                    .append('n');
            }
            if (this.italic) {
                builder.append(SECTION_SIGN)
                    .append('o');
            }
            return builder.toString();
        }

        private void reset() {
            this.color = 0;
            this.obfuscated = false;
            this.bold = false;
            this.strikethrough = false;
            this.underlined = false;
            this.italic = false;
        }
    }
}

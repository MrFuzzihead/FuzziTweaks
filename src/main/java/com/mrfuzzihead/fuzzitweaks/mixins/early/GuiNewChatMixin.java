package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.common.util.ChatLineSplitter;

/**
 * Splits a multiline chat message into one chat line per embedded newline.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock), but with a component-tree splitter instead of rebuilding plain text. The
 * splitter preserves component styles, click/hover events and raw {@code §} formatting at line boundaries, which the
 * original implementation dropped. Only the normal {@code printChatMessage} path (line id 0) is split; a nonzero id
 * is an updatable line, and duplicating that id across several lines would make the lines delete each other.
 */
@Mixin(GuiNewChat.class)
public abstract class GuiNewChatMixin {

    @WrapOperation(
        method = "printChatMessage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;printChatMessageWithOptionalDeletion(Lnet/minecraft/util/IChatComponent;I)V"),
        require = 1)
    private void fuzziTweaks$splitMultilineMessage(GuiNewChat instance, IChatComponent component, int chatLineID,
        Operation<Void> original) {
        if (chatLineID != 0 || component == null
            || component.getUnformattedText()
                .indexOf('\n') < 0) {
            original.call(instance, component, chatLineID);
            return;
        }

        for (IChatComponent line : ChatLineSplitter.split(component)) {
            original.call(instance, line, 0);
        }
    }
}

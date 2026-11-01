package com.mrfuzzihead.fuzzitweaks.mixins.early;

import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears a text field when it is right-clicked.
 *
 * <p>
 * Ported from LegacyFixes (MIT, unilock). Vanilla only handles left clicks in {@code GuiTextField#mouseClicked},
 * so right-clicking a field did nothing. This cancels the rest of that method for a right-click that lands on a
 * visible, enabled field; every other button, every click outside the field, and every disabled or invisible field
 * keeps vanilla behaviour. A modded text field that overrides {@code mouseClicked} without calling super is not
 * affected, because this injection only runs inside the superclass method.
 *
 * <p>
 * The field is deliberately not focused: focusing on right-click could steal focus from another field in GUIs that
 * use right-click for their own actions. The option exists because a few modded fields do exactly that.
 */
@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin {

    @Shadow
    public int xPosition;

    @Shadow
    public int yPosition;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    private boolean canLoseFocus;

    @Shadow
    private boolean isFocused;

    @Shadow
    private boolean isEnabled;

    @Shadow
    private boolean visible;

    @Shadow
    public abstract void setText(String text);

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void fuzziTweaks$clearOnRightClick(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 1 || !this.visible || !this.isEnabled || (!this.isFocused && !this.canLoseFocus)) {
            return;
        }
        if (mouseX < this.xPosition || mouseX >= this.xPosition + this.width) {
            return;
        }
        if (mouseY < this.yPosition || mouseY >= this.yPosition + this.height) {
            return;
        }

        this.setText("");
        ci.cancel();
    }
}

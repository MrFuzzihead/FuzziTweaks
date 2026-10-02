package com.mrfuzzihead.fuzzitweaks.mixins.late.thaumicadditions;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.pengu.thaumcraft.additions.utils.hud.HUD;

/**
 * Suppresses the Thaumic Additions HUD overlay.
 *
 * <p>
 * Thaumic Additions draws everything from a single method,
 * {@code com.pengu.thaumcraft.additions.utils.hud.HUD#render(RenderGameOverlayEvent$Pre)},
 * which is what paints the red-to-green Adaminite/wand gauge you see at the top of the
 * screen. Cancelling at HEAD removes the visual only.
 *
 * <p>
 * {@code remap = false} is required, not optional: the Mixin annotation processor resolves every
 * {@code @Inject} target against the game's SRG mapping file, and {@code HUD} is a Thaumic
 * Additions class rather than a Minecraft one, so it has no entry there. Without the flag the
 * build fails with "Unable to locate obfuscation mapping for @Inject target render". The method
 * name is only ever obfuscated for Minecraft classes, so skipping the remap is also the correct
 * thing to do, not just a workaround. The existing {@code KeyBindingsMixin} does the same.
 *
 * <p>
 * The full descriptor is spelled out so the overload cannot drift, and {@code require = 0} keeps
 * the mixin from failing to apply if a future Thaumic Additions build renames or reshapes the
 * method.
 *
 * <p>
 * Everything else in {@code HUD} is untouched — glyph loading ({@code onTextureReload}),
 * F3 debug text ({@code addF3Info}) and tip-message lifetime ({@code clientTick}) all still
 * run, so there are no NPEs from partially-initialized state.
 */
@Mixin(HUD.class)
public abstract class HUDMixin {

    @Inject(
        method = "render(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Pre;)V",
        at = @At("HEAD"),
        remap = false,
        cancellable = true,
        require = 0)
    private void tahud$suppressOverlay(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        if (Config.disableThaumicAdditionsEatingGraphic) {
            ci.cancel();
        }
    }
}

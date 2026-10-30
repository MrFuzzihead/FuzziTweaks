package com.mrfuzzihead.fuzzitweaks.mixins.late.thaumicadditions;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.pengu.thaumcraft.additions.utils.hud.HUD;

/**
 * Suppresses the Thaumic Additions eating/use progress bar.
 *
 * <p>
 * Thaumic Additions renders several unrelated things from one
 * {@code HUD#render(RenderGameOverlayEvent$Pre)} method: the Adaminite armor gauge, the
 * green-to-red item-use bar, tip messages and {@code IHUDRenderable} extras. Cancelling the
 * whole method at HEAD would hide all of them, so this redirects only the
 * {@code EntityPlayer#getItemInUse()} result that gates the item-use bar. It returns {@code null}
 * while the option is enabled, which leaves the rest of the HUD untouched.
 *
 * <p>
 * {@code remap = false} is required on the enclosing mixin annotation: the annotation processor
 * resolves {@code HUD} and its {@code render} method against the game's SRG mapping file, and
 * neither is a Minecraft class, so they have no entry there. The {@code @At.remap = true} keeps
 * the Minecraft {@code getItemInUse} call remapped. The existing {@code KeyBindingsMixin} uses
 * the same split for its Thaumic Additions target.
 *
 * <p>
 * The full descriptor is spelled out so the overload cannot drift, and {@code require = 0} keeps
 * the mixin from failing to apply if a future Thaumic Additions build renames or reshapes the
 * method.
 *
 * <p>
 * Everything else in {@code HUD} is untouched — glyph loading ({@code onTextureReload}),
 * F3 debug text ({@code addF3Info}), tip-message lifetime ({@code clientTick}) and the Adaminite
 * gauge all still run.
 */
@Mixin(HUD.class)
public abstract class HUDMixin {

    @Redirect(
        method = "render(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Pre;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;getItemInUse()Lnet/minecraft/item/ItemStack;",
            remap = true),
        remap = false,
        require = 0)
    private ItemStack tahud$hideEatingGraphic(EntityPlayer player) {
        if (Config.disableThaumicAdditionsEatingGraphic) {
            return null;
        }
        return player.getItemInUse();
    }
}

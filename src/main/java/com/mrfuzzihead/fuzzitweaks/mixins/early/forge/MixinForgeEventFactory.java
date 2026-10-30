package com.mrfuzzihead.fuzzitweaks.mixins.early.forge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.FuzziTweaks;
import com.mrfuzzihead.fuzzitweaks.common.util.RailcraftCartCompat;
import com.mrfuzzihead.fuzzitweaks.common.util.RailcraftPresence;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * Re-asserts {@code useItem = DENY} after a right-click-block interaction event has been fully posted.
 *
 * <p>
 * Railcraft's {@code MinecartHooks.onItemUse} denies the vanilla item use for the cart items it replaces, then
 * spawns its own cart. That deny is only an event result, though, so any handler that runs after it can undo it.
 * The common case is a mod calling {@code PlayerInteractEvent.setCanceled(false)}: in 1.7.10 that also resets
 * {@code useItem} and {@code useBlock} from {@link Event.Result#DENY} to {@link Event.Result#DEFAULT}. Assigning
 * {@code event.useItem = Event.Result.DEFAULT/ALLOW} has the same effect. When that happens
 * {@code ItemInWorldManager.activateBlockOrUseItem} sees a non-DENY result and lets the vanilla minecart item
 * place a second cart from the same click.
 * </p>
 *
 * <p>
 * Injecting at the return of {@link ForgeEventFactory#onPlayerInteract} runs after the entire event bus has
 * finished, so no handler can reset the result afterwards. This suppresses the vanilla placement only; Railcraft's
 * handler has already spawned its cart by then.
 * </p>
 *
 * <p>
 * This is also the diagnostic hook for tracking down the reset. With {@link Config#logMinecartPlacementDebug} on,
 * every minecart interaction logs the caller of {@code onPlayerInteract} and the result before this hook runs.
 * Two traces for one click mean another mod calls the factory a second time; a single trace with
 * {@code useItemBefore=DEFAULT} means a handler reset the result or the event was canceled before Railcraft ran.
 * A synthetic event posted directly to the event bus bypasses the factory and does not appear as a trace.
 * </p>
 *
 * <p>
 * {@code remap = false}: {@link ForgeEventFactory} is a Forge class, so its methods are not obfuscated and the
 * descriptor below uses the stable MCP class names. The full descriptor is required because
 * {@code onPlayerInteract} has a six-argument overload that delegates to this seven-argument one.
 * </p>
 */
@Mixin(value = ForgeEventFactory.class, remap = false)
public abstract class MixinForgeEventFactory {

    @Inject(
        method = "onPlayerInteract(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$Action;IIIILnet/minecraft/world/World;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;",
        at = @At("RETURN"),
        require = 1,
        remap = false)
    private static void fuzziTweaks$forceMinecartDeny(EntityPlayer player, PlayerInteractEvent.Action action, int x,
        int y, int z, int face, World world, CallbackInfoReturnable<PlayerInteractEvent> cir) {
        final PlayerInteractEvent event = cir.getReturnValue();
        if (event == null || action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || player == null) {
            return;
        }

        // Railcraft is optional at runtime. Keep this check in front of the helper call so packs without
        // Railcraft never resolve any Railcraft classes from this early mixin. The result is cached from
        // pre-init, after every mod has registered.
        if (!RailcraftPresence.isLoaded()) {
            return;
        }

        final ItemStack stack = player.getHeldItem();
        if (stack == null) {
            return;
        }

        final Item item = stack.getItem();
        if (item == null) {
            return;
        }

        final boolean replacedByRailcraft = RailcraftCartCompat.isReplacedCartItem(item);
        final boolean minecartItem = item instanceof ItemMinecart;

        // Log any minecart item, not only the ones Railcraft replaces, so an unexpected cart item in the trace
        // identifies the mod responsible for it.
        if (!replacedByRailcraft && !minecartItem) {
            return;
        }

        // Capture the result as the last event handler left it, before this hook overrides it, so the debug log
        // shows whether another mod actually reset Railcraft's DENY.
        final Event.Result useItemBefore = event.useItem;

        if (Config.enableRailcraftMinecartPlacementFix && replacedByRailcraft) {
            event.useItem = Event.Result.DENY;
        }

        if (Config.logMinecartPlacementDebug) {
            final String side = world == null ? "?" : world.isRemote ? "client" : "server";
            final String itemClass = item.getClass()
                .getName();
            FuzziTweaks.LOG.info(
                "[CartDebug] side={} item={} size={} useItemBefore={} useItem={} useBlock={} canceled={} "
                    + "pos={},{},{} face={} player={}",
                side,
                itemClass,
                stack.stackSize,
                useItemBefore,
                event.useItem,
                event.useBlock,
                event.isCanceled(),
                x,
                y,
                z,
                face,
                player.getCommandSenderName());
            FuzziTweaks.LOG
                .info("[CartDebug] caller of ForgeEventFactory.onPlayerInteract", new Throwable("[CartDebug] caller"));
        }
    }
}

package com.mrfuzzihead.fuzzitweaks.mixins.early;

import java.util.Map;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class TileEntityRendererDispatcherMixin {

    @Shadow
    public Map<Class<? extends TileEntity>, TileEntitySpecialRenderer> mapSpecialRenderers;

    @Shadow
    public World field_147550_f;

    /**
     * @author FuzziTweaks
     * @reason Replace the plain HashMap iterator with a snapshot-based iteration.
     *         The original code creates an Iterator over mapSpecialRenderers.values()
     *         and then calls tesr.func_147496_a(world) (setWorld) on each renderer.
     *         If any code path triggered by setWorld — or any concurrent thread — calls
     *         getSpecialRendererByClass for an unregistered TileEntity subclass, it will
     *         perform a put() on mapSpecialRenderers, invalidating the iterator and causing
     *         ConcurrentModificationException. Copying the values to an array first makes
     *         the iteration immune to those mutations.
     */
    @Overwrite
    public void func_147543_a(World world) {
        this.field_147550_f = world;

        TileEntitySpecialRenderer[] renderers = this.mapSpecialRenderers.values()
            .toArray(new TileEntitySpecialRenderer[0]);

        for (TileEntitySpecialRenderer tesr : renderers) {
            if (tesr != null) {
                tesr.func_147496_a(world);
            }
        }
    }
}

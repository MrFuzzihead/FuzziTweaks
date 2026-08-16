package com.mrfuzzihead.fuzzitweaks.mixins.early;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrfuzzihead.fuzzitweaks.client.ThreadedScreenshot;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @WrapOperation(method = "func_152348_aa",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/IChatComponent;"),
        require = 1)
    private IChatComponent captureScreenshot(File gameDirectory,
                                             int requestedWidthInPixels,
                                             int requestedHeightInPixels,
                                             Framebuffer frameBuffer,
                                             Operation<IChatComponent> original) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            return ThreadedScreenshot.capture(gameDirectory, frameBuffer);
        } else {
            return original.call(gameDirectory, requestedWidthInPixels, requestedHeightInPixels, frameBuffer);
        }
    }
}

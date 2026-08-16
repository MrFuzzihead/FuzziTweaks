package com.mrfuzzihead.fuzzitweaks.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Captures a screenshot on the render thread but performs the slow work (PNG
 * compression and writing to disk) on a background thread so that pressing the
 * screenshot key does not stall the client.
 *
 * <p>Timing is handled with a tiny state machine so that spamming the key while
 * a capture is already in flight is rejected instead of queuing unbounded work.
 * A client tick subscriber performs the (verified thread-safe) chat message
 * print on the main thread once the background thread has finished writing.
 *
 * <p>This is an original implementation of an approach popularised by the
 * (OSL-3.0) SwanSong mod; no source code from that project is used here.
 */
public final class ThreadedScreenshot {

    private static final int STATE_IDLE = 0;
    private static final int STATE_CAPTURING = 1;
    private static final int STATE_DONE = 2;

    private static final AtomicInteger state = new AtomicInteger(STATE_IDLE);

    private static volatile boolean resultSuccess;
    private static volatile File resultFile;
    private static volatile Exception resultException;

    private ThreadedScreenshot() {}

    /**
     * Registers the client-tick finalizer. Call once from the client proxy during
     * pre-init.
     */
    public static void init() {
        FMLCommonHandler.instance().bus().register(new ThreadedScreenshot());
    }

    /**
     * Grabs the current framebuffer pixels and hands the PNG write off to a
     * daemon thread. Must be called on the render thread (GL context).
     *
     * @return the chat message to show immediately, or an error message when we
     *         cannot take the capture.
     */
    public static IChatComponent capture(File gameDirectory, Framebuffer frameBuffer) {
        if (!state.compareAndSet(STATE_IDLE, STATE_CAPTURING)) {
            return new ChatComponentText("Already saving a screenshot, please wait.");
        }

        resultSuccess = false;
        resultFile = null;
        resultException = null;

        final int width = frameBuffer == null ? 0 : frameBuffer.framebufferWidth;
        final int height = frameBuffer == null ? 0 : frameBuffer.framebufferHeight;
        if (gameDirectory == null || frameBuffer == null || width <= 0 || height <= 0) {
            resultException = new IllegalStateException("Framebuffer was not ready to be captured.");
            state.set(STATE_IDLE);
            return new ChatComponentText("Failed to capture screenshot: " + resultException.getMessage());
        }

        final int[] argb;
        try {
            argb = readFramebuffer(frameBuffer, width, height);
        } catch (Exception e) {
            resultException = e;
            state.set(STATE_IDLE);
            return new ChatComponentText("Failed to capture screenshot: " + e.getMessage());
        }

        final File shotsDir = new File(gameDirectory, "screenshots");
        final File targetFile = freshScreenshotFile(shotsDir);

        final Thread writer = new Thread(() -> {
            try {
                shotsDir.mkdirs();
                final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, width, height, argb, 0, width);
                if (!ImageIO.write(image, "png", targetFile)) {
                    throw new IllegalStateException("No PNG writer was available.");
                }
                resultFile = targetFile;
                resultSuccess = true;
            } catch (Exception e) {
                resultSuccess = false;
                resultException = e;
            } finally {
                state.set(STATE_DONE);
            }
        });
        writer.setName("FuzziTweaks Screenshot Writer");
        writer.setDaemon(true);
        writer.start();

        return new ChatComponentText("Captured screenshot, saving in the background...");
    }

    /**
     * Picks the next free {@code screenshot-<timestamp>.png} file inside the
     * screenshots directory, following vanilla's naming scheme. Reimplements
     * {@code ScreenShotHelper}'s (package-private) filename logic so the heavy
     * write can run off the main thread while still producing compatible names.
     */
    private static File freshScreenshotFile(File shotsDir) {
        final String base = "screenshot-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        File candidate = new File(shotsDir, base + ".png");
        for (int i = 1; candidate.exists() && i < 50; i++) {
            candidate = new File(shotsDir, base + "_" + i + ".png");
        }
        return candidate;
    }

    /**
     * Copies the framebuffer's colour buffer into a vertically-flipped ARGB int
     * array. Runs on the render thread; the returned array is then safe to
     * consume from any thread.
     */
    private static int[] readFramebuffer(Framebuffer frameBuffer, int width, int height) {
        final int pixelCount = width * height;
        final ByteBuffer pixels = GLAllocation.createDirectByteBuffer(pixelCount * 4);

        frameBuffer.bindFramebuffer(false);
        try {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        }

        // OpenGL origin is bottom-left; build the scanlines top-down as
        // TYPE_INT_ARGB pixels so the written image is not upside down.
        final int[] argb = new int[pixelCount];
        for (int y = 0; y < height; y++) {
            final int srcRow = (height - 1 - y) * width;
            for (int x = 0; x < width; x++) {
                final int src = (srcRow + x) * 4;
                final int r = pixels.get(src) & 0xFF;
                final int g = pixels.get(src + 1) & 0xFF;
                final int b = pixels.get(src + 2) & 0xFF;
                final int a = pixels.get(src + 3) & 0xFF;
                argb[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return argb;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || state.get() != STATE_DONE) {
            return;
        }
        try {
            if (resultSuccess && resultFile != null) {
                final ChatComponentText fileName = new ChatComponentText(resultFile.getName());
                fileName.getChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, resultFile.getAbsolutePath()));
                Minecraft.getMinecraft().ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentText("Screenshot saved: ").appendSibling(fileName));
            } else {
                final String reason = resultException == null ? "Unknown error" : resultException.getMessage();
                Minecraft.getMinecraft().ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentText("Failed to save screenshot: " + reason));
            }
        } finally {
            resultFile = null;
            resultException = null;
            resultSuccess = false;
            state.set(STATE_IDLE);
        }
    }
}


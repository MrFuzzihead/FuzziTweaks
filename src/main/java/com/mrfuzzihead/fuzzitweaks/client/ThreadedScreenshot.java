package com.mrfuzzihead.fuzzitweaks.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Captures a screenshot on the render thread but performs the slow work (PNG
 * compression and writing to disk) on a background thread so that pressing the
 * screenshot key does not stall the client.
 *
 * <p>
 * Timing is handled with a tiny state machine so that spamming the key while
 * a capture is already in flight is rejected instead of queuing unbounded work.
 * A client tick subscriber performs the (verified thread-safe) chat message
 * print on the main thread once the background thread has finished writing.
 *
 * <p>
 * This is an original implementation of an approach popularised by the
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

    /**
     * Reused capture buffers. The state machine prevents a second capture until the writer has finished with the
     * array, so reusing them cannot race with the background PNG write.
     */
    private static IntBuffer pixelBuffer;
    private static int[] pixelValues;

    private ThreadedScreenshot() {}

    /**
     * Registers the client-tick finalizer. Call once from the client proxy during
     * pre-init.
     */
    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ThreadedScreenshot());
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
                // TYPE_INT_RGB drops the framebuffer alpha, matching vanilla's ScreenShotHelper.
                final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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
        final String base = "screenshot-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT).format(new Date());
        File candidate = new File(shotsDir, base + ".png");
        for (int i = 1; candidate.exists(); i++) {
            candidate = new File(shotsDir, base + "_" + i + ".png");
        }
        return candidate;
    }

    /**
     * Copies the framebuffer's colour buffer into a vertically-flipped packed
     * ARGB array. Runs on the render thread; the returned array is then safe to
     * consume from any thread.
     */
    private static int[] readFramebuffer(Framebuffer frameBuffer, int width, int height) {
        final int pixelCount = width * height;
        if (pixelBuffer == null || pixelBuffer.capacity() < pixelCount) {
            pixelBuffer = BufferUtils.createIntBuffer(pixelCount);
            pixelValues = new int[pixelCount];
        }

        frameBuffer.bindFramebuffer(false);
        try {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            pixelBuffer.clear();
            GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        }

        // Same packed format and row flip as vanilla's ScreenShotHelper. The
        // writer uses TYPE_INT_RGB, so alpha is dropped like the vanilla path.
        pixelBuffer.position(0);
        pixelBuffer.limit(pixelCount);
        pixelBuffer.get(pixelValues, 0, pixelCount);
        TextureUtil.func_147953_a(pixelValues, width, height);
        return pixelValues;
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

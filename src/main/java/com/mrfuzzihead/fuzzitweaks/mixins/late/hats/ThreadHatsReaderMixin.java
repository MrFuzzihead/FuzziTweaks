package com.mrfuzzihead.fuzzitweaks.mixins.late.hats;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.common.util.HatsCdn;

import hats.common.thread.ThreadHatsReader;

/**
 * Repoints the Hats hat-repository download at a live mirror.
 *
 * <p>
 * {@link ThreadHatsReader#run()} opens the hat manifest from a hardcoded URL
 * ({@code http://www.creeperrepo.net/ichun/static/hats.xml}) and then opens every {@code <URL>} element it
 * finds in that manifest. The domain has expired (it now parks and answers with a JavaScript redirect page),
 * which makes the manifest parse throw inside the thread's {@code catch (Exception)} block. The exception is
 * only printed, so the game keeps running with an empty hat list - no hats on players, no hats on mobs, and an
 * empty selection GUI.
 * </p>
 *
 * <p>
 * The URLs themselves are built with {@code new URL(String)}, but that constructor cannot be hooked: GTNH's
 * Mixin fork rejects any {@code @Redirect} whose target is a {@code <init>} with
 * {@code "Illegal @Redirect of constructor specified on ..."} from {@code RedirectInjector.addTargetNode}.
 * Both of the URLs are only ever used through {@link URL#openConnection()} - once directly in {@code run()} for
 * the manifest, once in {@link ThreadHatsReader#downloadResource} for each hat file - so redirecting those two
 * calls covers the entire download path while leaving the mod's own timeout and parsing code untouched.
 * {@link HatsCdn#openConnection(URL)} swaps the host for the configured mirror and keeps the path, which is
 * exactly what the defunct hosts and their successor agree on.
 * </p>
 *
 * <p>
 * The second redirect below is unrelated to the dead domain but just as important once the download actually
 * runs: the whole manifest loop sits inside a single {@code try/catch (Exception)} in {@code run()}, so a
 * single failing hat - one timeout, one 5xx from the CDN - aborts every hat after it in the manifest and only
 * leaves a stack trace behind. Since the download walks roughly four hundred URLs one by one over a third
 * party CDN, that is a realistic failure, and it looks exactly like "the hats stopped part way through the
 * alphabet".
 * </p>
 */
@Mixin(value = ThreadHatsReader.class, remap = false)
public abstract class ThreadHatsReaderMixin {

    /**
     * Redirects the manifest connection opened in {@code run()}.
     */
    @Redirect(
        method = "run",
        at = @At(value = "INVOKE", target = "Ljava/net/URL;openConnection()Ljava/net/URLConnection;"))
    private URLConnection fuzziTweaks$rebaseManifestConnection(URL url) throws IOException {
        return HatsCdn.openConnection(url);
    }

    /**
     * Redirects the per-hat connection opened in {@code downloadResource}. Required in addition to the manifest
     * redirect: the URLs inside the manifest point at {@code http://cdn.redstone.tech}, which redirects to
     * HTTPS, and {@code HttpURLConnection} does not follow protocol-changing redirects.
     */
    @Redirect(
        method = "downloadResource",
        at = @At(value = "INVOKE", target = "Ljava/net/URL;openConnection()Ljava/net/URLConnection;"))
    private URLConnection fuzziTweaks$rebaseHatFileConnection(URL url) throws IOException {
        return HatsCdn.openConnection(url);
    }

    /**
     * Keeps one unusable hat from costing the rest of the manifest by containing failures to the file they
     * happened on: the mod's own loop has no per-entry error handling, so without this the exception escapes
     * into {@code run()}'s catch-all and every remaining entry is skipped in silence.
     *
     * <p>
     * Skipping is safe and self-healing on its own terms - {@code downloadResource} re-downloads anything whose
     * size does not match and skips files that are already present and readable, so the missing hat is simply
     * picked up on the next launch.
     * </p>
     */
    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lhats/common/thread/ThreadHatsReader;downloadResource(Ljava/net/URL;Ljava/io/File;J)Z"))
    private boolean fuzziTweaks$downloadOneAtATime(ThreadHatsReader reader, URL url, File target, long size) {
        try {
            return reader.downloadResource(url, target, size);
        } catch (Exception e) {
            // Deliberately plain stderr, like the mod's own reporting: its download thread only ever
            // printStackTrace()s, and Hats.console() would pull iChunUtil onto the compile classpath.
            System.err.println("[FuzziTweaks] Hats: skipped " + target.getName() + ", continuing. " + e);
            return false;
        }
    }
}

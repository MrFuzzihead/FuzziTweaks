package com.mrfuzzihead.fuzzitweaks.mixins.late.hats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.fuzzitweaks.Config;
import com.mrfuzzihead.fuzzitweaks.common.util.HatsCdn;

import hats.common.core.HatHandler;
import hats.common.thread.ThreadGetModMobSupport;

/**
 * Makes the Hats "mod mob support" list survive its dead online source.
 *
 * <p>
 * {@link ThreadGetModMobSupport#run()} reads {@code HatModMobSupport.json} either from the local {@code hats/}
 * folder (when {@code readLocalModMobSupport} is {@code 1} in {@code Hats.cfg}) or from
 * {@code https://raw.github.com/iChun/Hats/master/src/main/resources/assets/hats/mod/HatModMobSupport.json}.
 * That raw GitHub path no longer exists in the repository - it returns {@code 404} - so the online branch
 * throws, the exception is swallowed, and no modded mobs ever get hat placement data.
 * </p>
 *
 * <p>
 * Rather than depend on the pack owner remembering to flip a setting in another mod's config, this redirect
 * quietly serves the local file whenever it is present. If it is not there, the original stream request is
 * performed and behaviour is unchanged, so nothing is lost compared to the unpatched mod. The hook sits on
 * {@link URL#openStream()} rather than on the {@code new URL(String)} next to it because GTNH's Mixin fork
 * rejects {@code @Redirect} of a constructor.
 * </p>
 */
@Mixin(value = ThreadGetModMobSupport.class, remap = false)
public abstract class ThreadGetModMobSupportMixin {

    /**
     * Redirects the stream opened in {@code run()} to the local {@code hats/HatModMobSupport.json} when that
     * file exists.
     */
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Ljava/net/URL;openStream()Ljava/io/InputStream;"))
    private InputStream fuzziTweaks$localMobSupportStream(URL url) throws IOException {
        final File local = fuzziTweaks$localModMobSupportFile();
        if (local != null) {
            return new FileInputStream(local);
        }

        return HatsCdn.openStream(url);
    }

    /**
     * @return the local {@code hats/HatModMobSupport.json} if it is a readable file, otherwise {@code null}.
     *         Uses the folder Hats resolved for itself, falling back to the game directory.
     */
    private File fuzziTweaks$localModMobSupportFile() {
        if (!Config.preferLocalHatMobSupport) {
            return null;
        }

        final File hatsFolder = HatHandler.hatsFolder != null ? HatHandler.hatsFolder
            : new File(Launch.minecraftHome, "hats");
        final File json = new File(hatsFolder, "HatModMobSupport.json");

        return json.isFile() ? json : null;
    }
}

package com.mrfuzzihead.fuzzitweaks.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Endpoint rewriter for the Hats mod hat repository.
 *
 * <p>
 * {@code hats.common.thread.ThreadHatsReader} hardcodes its manifest URL as
 * {@code http://www.creeperrepo.net/ichun/static/hats.xml}. That domain has expired and is now parked: it
 * answers {@code 302} with a redirect to {@code survey-smiles.com} and a JavaScript challenge page instead of
 * XML, so the {@code DocumentBuilder.parse} in the reader thread throws, gets swallowed by the surrounding
 * {@code catch (Exception)} block, and the mod silently ends up with zero hats.
 * </p>
 *
 * <p>
 * The same content is still served by CreeperHost's successor CDN at {@code http://dist.creeper.host}. Both
 * the old manifest location ({@code /ichun/static/hats.xml}) and the hat files themselves
 * ({@code /ichun/hats/<pack>/<file>.tc2}) keep an identical path on the new host, so the whole fix is to keep
 * the path and swap the {@code scheme://host} part.
 * </p>
 *
 * <p>
 * Rewriting both the manifest and the per-file URLs also sidesteps a second problem: the {@code <URL>}
 * elements inside the manifest point at {@code http://cdn.redstone.tech}, which {@code 301} redirects to
 * {@code https://dist.creeper.host}. {@code HttpURLConnection} does not follow redirects that change protocol,
 * so downloading through the manifest URLs fails even once the manifest itself parses. Pointing the file URLs
 * straight at plain HTTP avoids the redirect entirely.
 * </p>
 */
public final class HatsCdn {

    /**
     * Domains that served (or redirect to) the Hats hat repository at some point and can no longer be reached
     * directly. Matched by suffix, so {@code www.} and other subdomains are covered as well.
     */
    private static final String[] LEGACY_HOSTS = { "creeperrepo.net", "redstone.tech", "dist.creeper.host",
        "filedist.ch" };

    private HatsCdn() {}

    /**
     * Swaps the {@code scheme://host[:port]} part of {@code url} for {@link Config#hatsDownloadBaseUrl} when
     * the URL points at one of the known Hats repository hosts. Anything else (a self-hosted mirror the user
     * configured in {@code Hats.cfg}-adjacent tooling, a mod-provided URL, ...) is returned untouched.
     *
     * <p>
     * The path and everything after it are copied verbatim, including the {@code %20} escapes the mod applies
     * to hat file names, so nothing is double-encoded.
     * </p>
     *
     * @param spec
     *             the URL Hats was about to open.
     * @return the rebased URL, or {@code spec} unchanged when the host is unknown or the base URL is blank.
     */
    public static String rebaseUrl(String spec) {
        if (spec == null) {
            return null;
        }

        final String base = Config.hatsDownloadBaseUrl;
        if (base == null || base.trim()
            .isEmpty()) {
            return spec;
        }

        final int schemeEnd = spec.indexOf("://");
        if (schemeEnd < 0) {
            return spec;
        }

        final int pathStart = spec.indexOf('/', schemeEnd + 3);
        final String authority = pathStart < 0 ? spec.substring(schemeEnd + 3)
            : spec.substring(schemeEnd + 3, pathStart);
        if (!isLegacyHost(authority)) {
            return spec;
        }

        String trimmedBase = base.trim();
        while (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }

        return pathStart < 0 ? trimmedBase : trimmedBase + spec.substring(pathStart);
    }

    /**
     * Convenience wrapper for call sites that already hold a {@link URL}: returns {@code url} itself when it is
     * not a legacy repository URL, so no pointless copy is made and {@code equals}/{@code toString} behaviour is
     * unchanged for everything we do not rewrite.
     *
     * <p>
     * A malformed rewrite degrades to the original URL rather than blowing up a mod thread: the caller then
     * fails exactly the way it would have without this mod.
     * </p>
     */
    public static URL rebase(URL url) {
        if (url == null) {
            return null;
        }

        final String spec = url.toString();
        final String rebased = rebaseUrl(spec);
        if (rebased.equals(spec)) {
            return url;
        }

        try {
            return new URL(rebased);
        } catch (MalformedURLException e) {
            return url;
        }
    }

    /**
     * Opens {@code url}, rebased onto {@link Config#hatsDownloadBaseUrl} when it names a legacy repository host.
     *
     * <p>
     * This is the entry point the Hats mixins hook, because GTNH's Mixin fork refuses to {@code @Redirect}
     * constructors - {@code RedirectInjector.addTargetNode} throws {@code "Illegal @Redirect of constructor
     * specified on ..."} for any {@code <init>} target. Opening the connection is the first call Hats makes on
     * the URL, and the only one, so hooking it covers the whole download.
     * </p>
     */
    public static URLConnection openConnection(URL url) throws IOException {
        return rebase(url).openConnection();
    }

    /** @see #openConnection(URL) */
    public static InputStream openStream(URL url) throws IOException {
        return rebase(url).openStream();
    }

    /** @return {@code true} when the {@code scheme://host[:port]} authority names a known Hats repository host. */
    public static boolean isLegacyHost(String authority) {
        if (authority == null) {
            return false;
        }

        String host = authority.toLowerCase(Locale.ROOT);

        // Strip userinfo and port before comparing.
        final int userInfoEnd = host.lastIndexOf('@');
        if (userInfoEnd >= 0) {
            host = host.substring(userInfoEnd + 1);
        }
        final int portStart = host.indexOf(':');
        if (portStart >= 0) {
            host = host.substring(0, portStart);
        }

        for (String legacy : LEGACY_HOSTS) {
            if (host.equals(legacy) || host.endsWith("." + legacy)) {
                return true;
            }
        }
        return false;
    }
}

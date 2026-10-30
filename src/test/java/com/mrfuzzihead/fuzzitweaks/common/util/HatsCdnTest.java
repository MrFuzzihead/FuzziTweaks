package com.mrfuzzihead.fuzzitweaks.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mrfuzzihead.fuzzitweaks.Config;

/**
 * Exercises the pure URL rewriting used by the Hats endpoint fix.
 */
class HatsCdnTest {

    private String originalBase;

    @BeforeEach
    void saveBase() {
        originalBase = Config.hatsDownloadBaseUrl;
    }

    @AfterEach
    void restoreBase() {
        Config.hatsDownloadBaseUrl = originalBase;
    }

    @Test
    void rebasesEveryKnownLegacyHost() {
        Config.hatsDownloadBaseUrl = "https://mirror.example";

        assertEquals(
            "https://mirror.example/ichun/static/hats.xml",
            HatsCdn.rebaseUrl("http://www.creeperrepo.net/ichun/static/hats.xml"));
        assertEquals(
            "https://mirror.example/ichun/hats/pack/file.tc2",
            HatsCdn.rebaseUrl("https://cdn.redstone.tech/ichun/hats/pack/file.tc2"));
        assertEquals(
            "https://mirror.example/ichun/static/hats.xml",
            HatsCdn.rebaseUrl("http://dist.creeper.host/ichun/static/hats.xml"));
        assertEquals(
            "https://mirror.example/ichun/static/hats.xml",
            HatsCdn.rebaseUrl("http://filedist.ch/ichun/static/hats.xml"));
    }

    @Test
    void preservesPathQueryAndFragmentWhenRebasing() {
        Config.hatsDownloadBaseUrl = "http://127.0.0.1:8080";

        assertEquals(
            "http://127.0.0.1:8080/ichun/hats/a%20b.tc2?raw=1#top",
            HatsCdn.rebaseUrl("http://user:pass@dist.creeper.host:9000/ichun/hats/a%20b.tc2?raw=1#top"));
    }

    @Test
    void leavesUnknownOrUnparseableUrlsAlone() {
        Config.hatsDownloadBaseUrl = "https://mirror.example";

        assertEquals("https://example.com/hats.xml", HatsCdn.rebaseUrl("https://example.com/hats.xml"));
        assertEquals("not a URL", HatsCdn.rebaseUrl("not a URL"));
        assertNull(HatsCdn.rebaseUrl(null));
    }

    @Test
    void blankOrMissingBaseDisablesRewriting() {
        final String legacyUrl = "http://www.creeperrepo.net/ichun/static/hats.xml";

        Config.hatsDownloadBaseUrl = "";
        assertEquals(legacyUrl, HatsCdn.rebaseUrl(legacyUrl));

        Config.hatsDownloadBaseUrl = null;
        assertEquals(legacyUrl, HatsCdn.rebaseUrl(legacyUrl));
    }

    @Test
    void handlesUrlsWithoutAPath() {
        Config.hatsDownloadBaseUrl = "https://mirror.example";

        assertEquals("https://mirror.example", HatsCdn.rebaseUrl("http://dist.creeper.host"));
    }

    @Test
    void rebaseUrlPreservesTheUrlObjectWhenNothingChanges() {
        Config.hatsDownloadBaseUrl = "https://mirror.example";
        final URL unknown = newUrl("https://example.com/hats.xml");

        assertSame(unknown, HatsCdn.rebase(unknown));
    }

    @Test
    void rebaseUrlReturnsARebasedCopyForLegacyHosts() {
        Config.hatsDownloadBaseUrl = "https://mirror.example";

        assertEquals(
            "https://mirror.example/ichun/static/hats.xml",
            HatsCdn.rebase(newUrl("http://www.creeperrepo.net/ichun/static/hats.xml"))
                .toString());
    }

    @Test
    void isLegacyHostMatchesKnownDomainsAndSubdomains() {
        assertTrue(HatsCdn.isLegacyHost("creeperrepo.net"));
        assertTrue(HatsCdn.isLegacyHost("www.creeperrepo.net"));
        assertTrue(HatsCdn.isLegacyHost("CDN.REDSTONE.TECH:443"));
        assertTrue(HatsCdn.isLegacyHost("user@dist.creeper.host:8080"));
        assertTrue(HatsCdn.isLegacyHost("filedist.ch"));

        assertFalse(HatsCdn.isLegacyHost("notcreeperrepo.net"));
        assertFalse(HatsCdn.isLegacyHost("creeperrepo.net.evil"));
        assertFalse(HatsCdn.isLegacyHost("example.com"));
        assertFalse(HatsCdn.isLegacyHost(null));
    }

    private static URL newUrl(String spec) {
        try {
            return new URL(spec);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}

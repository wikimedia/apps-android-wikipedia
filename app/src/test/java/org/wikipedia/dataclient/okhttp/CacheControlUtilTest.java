package org.wikipedia.dataclient.okhttp;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CacheControlUtilTest {
    @Test public void testReplaceMaxStale() {
        assertThat(CacheControlUtil.replaceMaxStale("no-cache, max-stale=1, max-age=2, max-stale=3",
                10), is("no-cache, max-age=2, max-stale=10"));
    }

    @Test public void testReplaceDirectiveEmpty() {
        assertThat(CacheControlUtil.replaceDirective("", "no-cache", "no-cache"), is("no-cache"));
    }

    @Test public void testReplaceDirectiveSingular() {
        assertThat(CacheControlUtil.replaceDirective("max-age=1", "no-cache", "no-cache"),
                is("max-age=1, no-cache"));
    }

    @Test public void testReplaceDirectiveMultiple() {
        assertThat(CacheControlUtil.replaceDirective("max-age=1, max-stale=2", "no-cache", "no-cache"),
                is("max-age=1, max-stale=2, no-cache"));
    }

    @Test public void testReplaceDirectiveRedundantSingular() {
        assertThat(CacheControlUtil.replaceDirective("no-cache", "no-cache", "no-cache"),
                is("no-cache"));
    }

    @Test public void testReplaceDirectiveRedundantLeading() {
        assertThat(CacheControlUtil.replaceDirective("no-cache, max-age=1", "no-cache", "no-cache"),
                is("max-age=1, no-cache"));
    }

    @Test public void testReplaceDirectiveRedundantMiddle() {
        assertThat(CacheControlUtil.replaceDirective("max-age=1, no-cache, max-stale=2", "no-cache",
                "no-cache"), is("max-age=1, max-stale=2, no-cache"));
    }

    @Test public void testReplaceDirectiveRedundantTrailing() {
        assertThat(CacheControlUtil.replaceDirective("max-age=1, max-stale=2, no-cache", "no-cache",
                "no-cache"), is("max-age=1, max-stale=2, no-cache"));
    }

    @Test public void testReplaceDirectiveMultipleRedundant() {
        assertThat(CacheControlUtil.replaceDirective("no-cache, max-age=1, no-cache, max-stale=2",
                "no-cache", "no-cache"), is("max-age=1, max-stale=2, no-cache"));
    }
}

package org.wikipedia.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriUtilTest {
    /**
     * Inspired by
     * curl -s https://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&format=json&siprop=general | jq .query.general.legaltitlechars
     */
    private static final String TITLE
            = " %!\"$&'()*,\\-.\\/0-9:;=?@A-Z\\\\^_`a-z~\\x80-\\xFF+";

    /**
     * Inspired by
     *from http://stackoverflow.com/questions/2849756/list-of-valid-characters-for-the-fragment-identifier-in-an-url
     */
    private static final String LEGAL_FRAGMENT_CHARS
            = "!$&'()*+,;=-._~:@/?abc0123456789%D8%f6";

    @Test
    public void testRemoveFragment() {
        assertThat(UriUtil.removeFragment(TITLE + "#" + LEGAL_FRAGMENT_CHARS), is(TITLE));
    }

    @Test
    public void testRemoveEmptyFragment() {
        assertThat(UriUtil.removeFragment(TITLE + "#"), is(TITLE));
    }

    @Test
    public void testRemoveFragmentWithHash() {
        assertThat(UriUtil.removeFragment(TITLE + "##"), is(TITLE));
    }

    @Test
    public void testEncodeURIComponent() {
        assertThat(UriUtil.encodeURIComponent("Unencoded"), is("Unencoded"));
        assertThat(UriUtil.encodeURIComponent("Unencoded?!"), is("Unencoded%3F!"));
        assertThat(UriUtil.encodeURIComponent("C++11"), is("C%2B%2B11"));
        assertThat(UriUtil.encodeURIComponent("Foo bar"), is("Foo%20bar"));
        assertThat(UriUtil.encodeURIComponent("abcdefghijklmnopqrstuvwxyz0123456789`-=~!@#$%^&*()_+[]{}<>,./?|\\"),
                is("abcdefghijklmnopqrstuvwxyz0123456789%60-%3D~!%40%23%24%25%5E%26*()_%2B%5B%5D%7B%7D%3C%3E%2C.%2F%3F%7C%5C"));
    }

    @Test
    public void testDecodeURIComponent() {
        assertThat(UriUtil.decodeURIComponent(UriUtil.encodeURIComponent("Unencoded")), is("Unencoded"));
        assertThat(UriUtil.decodeURIComponent(UriUtil.encodeURIComponent("Unencoded?!")), is("Unencoded?!"));
        assertThat(UriUtil.decodeURIComponent(UriUtil.encodeURIComponent("C++11")), is("C++11"));
        assertThat(UriUtil.decodeURIComponent(UriUtil.encodeURIComponent("Foo bar")), is("Foo bar"));
        assertThat(UriUtil.decodeURIComponent(UriUtil.encodeURIComponent("abcdefghijklmnopqrstuvwxyz0123456789`-=~!@#$%^&*()_+[]{}<>,./?|\\")),
                is("abcdefghijklmnopqrstuvwxyz0123456789`-=~!@#$%^&*()_+[]{}<>,./?|\\"));
    }
}

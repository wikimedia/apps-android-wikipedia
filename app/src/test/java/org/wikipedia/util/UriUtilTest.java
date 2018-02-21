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
}

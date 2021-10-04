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
        assertThat(UriUtil.encodeURL("Unencoded"), is("Unencoded"));
        assertThat(UriUtil.encodeURL("C++"), is("C%2B%2B"));
        assertThat(UriUtil.encodeURL("100%"), is("100%25"));
        assertThat(UriUtil.encodeURL("Википедия"), is("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F"));
        assertThat(UriUtil.encodeURL("Sentence with spaces"), is("Sentence%20with%20spaces"));
    }

    @Test
    public void testDecodeURIComponent() {
        assertThat(UriUtil.decodeURL("Unencoded"), is("Unencoded"));
        assertThat(UriUtil.decodeURL("C++"), is("C++"));
        assertThat(UriUtil.decodeURL("C%2B%2B"), is("C++"));
        assertThat(UriUtil.decodeURL("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F"), is("Википедия"));
        assertThat(UriUtil.decodeURL("Sentence%20with%20spaces"), is("Sentence with spaces"));
    }

    @Test
    public void testRemoveInternalLinkPrefix() {
        assertThat(UriUtil.removeInternalLinkPrefix("/wiki/核武器"), is("核武器"));
        assertThat(UriUtil.removeInternalLinkPrefix("/zh/核武器"), is("核武器"));
        assertThat(UriUtil.removeInternalLinkPrefix("/zh-tw/核武器"), is("核武器"));
        assertThat(UriUtil.removeInternalLinkPrefix("/zh-hant/核武器"), is("核武器"));
        assertThat(UriUtil.removeInternalLinkPrefix("/sr-ec/Барак_Обама"), is("Барак_Обама"));
        assertThat(UriUtil.removeInternalLinkPrefix("/sr-el/Барак_Обама"), is("Барак_Обама"));
        assertThat(UriUtil.removeInternalLinkPrefix("/crh-cyrl/Bağçasaray"), is("Bağçasaray"));
    }

    @Test
    public void testRemoveLinkPrefix() {
        assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/wiki/核武器"), is("核武器"));
        assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh/核武器"), is("核武器"));
        assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-tw/核武器"), is("核武器"));
        assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-hant/核武器"), is("核武器"));
        assertThat(UriUtil.removeLinkPrefix("https://zh-tw.wikipedia.org/wiki/核武器"), is("核武器"));
        assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr/Барак_Обама"), is("Барак_Обама"));
        assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-el/Барак_Обама"), is("Барак_Обама"));
        assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-ec/Барак_Обама"), is("Барак_Обама"));
    }

    @Test
    public void testFilenameFromUploadUrl() {
        assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Oaxaca_in_Mexico.svg/320px-Oaxaca_in_Mexico.svg.png"), is("Oaxaca_in_Mexico.svg"));
        assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"), is("President_Barack_Obama.jpg"));
        assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/8/8d/President_Barack_Obama.jpg"), is("President_Barack_Obama.jpg"));
        assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/en/thumb/0/0d/Avengers_Endgame_poster.jpg/216px-Avengers_Endgame_poster.jpg"), is("Avengers_Endgame_poster.jpg"));
        assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Needle_Galaxy_4565.jpeg/320px-Needle_Galaxy_4565.jpeg"), is("Needle_Galaxy_4565.jpeg"));
        assertThat(UriUtil.getFilenameFromUploadUrl(""), is(""));
    }

    @Test
    public void testParseTalkTopicFromFragment() {
        assertThat(UriUtil.parseTalkTopicFromFragment("c-Dmitry_Brant-2021-10-01T12:36:00.000Z-test"), is("test"));
        assertThat(UriUtil.parseTalkTopicFromFragment("c-Dmitry_Brant-2021-10-01T12:36:00.000Z-test-1-2-3"), is("test-1-2-3"));
        assertThat(UriUtil.parseTalkTopicFromFragment("test"), is("test"));
    }
}

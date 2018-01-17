package org.wikipedia.page;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.wikipedia.page.Namespace.FILE;
import static org.wikipedia.page.Namespace.MAIN;
import static org.wikipedia.page.Namespace.MEDIA;
import static org.wikipedia.page.Namespace.SPECIAL;
import static org.wikipedia.page.Namespace.TALK;

@RunWith(RobolectricTestRunner.class) public class NamespaceTest {
    private static Locale PREV_DEFAULT_LOCALE;

    @BeforeClass public static void setUp() {
        PREV_DEFAULT_LOCALE = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterClass public static void tearDown() {
        Locale.setDefault(PREV_DEFAULT_LOCALE);
    }

    @Test public void testOf() {
        assertThat(Namespace.of(SPECIAL.code()), is(SPECIAL));
    }

    @Test public void testFromLegacyStringMain() {
        //noinspection deprecation
        assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("test"), null), is(MAIN));
    }

    @Test public void testFromLegacyStringFile() {
        //noinspection deprecation
        assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("he"), "קובץ"), is(FILE));
    }

    @Test public void testFromLegacyStringSpecial() {
        //noinspection deprecation
        assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("lez"), "Служебная"), is(SPECIAL));
    }

    @Test public void testFromLegacyStringTalk() {
        //noinspection deprecation
        assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "stringTalk"), is(TALK));
    }

    @Test public void testCode() {
        assertThat(MAIN.code(), is(0));
        assertThat(TALK.code(), is(1));
    }

    @Test public void testSpecial() {
        assertThat(SPECIAL.special(), is(true));
        assertThat(MAIN.special(), is(false));
    }

    @Test public void testMain() {
        assertThat(MAIN.main(), is(true));
        assertThat(TALK.main(), is(false));
    }

    @Test public void testFile() {
        assertThat(FILE.file(), is(true));
        assertThat(MAIN.file(), is(false));
    }

    @Test public void testTalkNegative() {
        assertThat(MEDIA.talk(), is(false));
        assertThat(SPECIAL.talk(), is(false));
    }

    @Test public void testTalkZero() {
        assertThat(MAIN.talk(), is(false));
    }

    @Test public void testTalkOdd() {
        assertThat(TALK.talk(), is(true));
    }

    @Test public void testToLegacyStringMain() {
        //noinspection deprecation
        assertThat(MAIN.toLegacyString(), nullValue());
    }

    @Test public void testToLegacyStringNonMain() {
        //noinspection deprecation
        assertThat(TALK.toLegacyString(), is("Talk"));
    }
}

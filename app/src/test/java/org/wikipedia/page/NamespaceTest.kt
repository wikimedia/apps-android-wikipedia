package org.wikipedia.page

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite

@RunWith(RobolectricTestRunner::class)
class NamespaceTest {

    @Test
    fun testOf() {
        MatcherAssert.assertThat(Namespace.of(Namespace.SPECIAL.code()), Matchers.`is`(Namespace.SPECIAL))
    }

    @Test
    fun testFromLegacyStringMain() {
        MatcherAssert.assertThat(
            Namespace.fromLegacyString(WikiSite.forLanguageCode("test"), null),
            Matchers.`is`(Namespace.MAIN)
        )
    }

    @Test
    fun testFromLegacyStringFile() {
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("he"), "קובץ"),
            Matchers.`is`(Namespace.FILE))
    }

    @Test
    fun testFromLegacyStringSpecial() {
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("lez"), "Служебная"),
            Matchers.`is`(Namespace.SPECIAL))
    }

    @Test
    fun testFromLegacyStringTalk() {
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "Talk"),
            Matchers.`is`(Namespace.TALK))
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("ru"), "Обсуждение"),
            Matchers.`is`(Namespace.TALK))
    }

    @Test
    fun testFromLegacyStringUser() {
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "User"),
            Matchers.`is`(Namespace.USER))
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("af"), "Gebruiker"),
            Matchers.`is`(Namespace.USER))
    }

    @Test
    fun testFromLegacyStringUserTalk() {
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "User talk"),
            Matchers.`is`(Namespace.USER_TALK))
        MatcherAssert.assertThat(Namespace.fromLegacyString(WikiSite.forLanguageCode("vi"), "Thảo luận Thành viên"),
            Matchers.`is`(Namespace.USER_TALK))
    }

    @Test
    fun testCode() {
        MatcherAssert.assertThat(Namespace.MAIN.code(), Matchers.`is`(0))
        MatcherAssert.assertThat(Namespace.TALK.code(), Matchers.`is`(1))
    }

    @Test
    fun testSpecial() {
        MatcherAssert.assertThat(Namespace.SPECIAL.special(), Matchers.`is`(true))
        MatcherAssert.assertThat(Namespace.MAIN.special(), Matchers.`is`(false))
    }

    @Test
    fun testMain() {
        MatcherAssert.assertThat(Namespace.MAIN.main(), Matchers.`is`(true))
        MatcherAssert.assertThat(Namespace.TALK.main(), Matchers.`is`(false))
    }

    @Test
    fun testFile() {
        MatcherAssert.assertThat(Namespace.FILE.file(), Matchers.`is`(true))
        MatcherAssert.assertThat(Namespace.MAIN.file(), Matchers.`is`(false))
    }

    @Test
    fun testTalkNegative() {
        MatcherAssert.assertThat(Namespace.MEDIA.talk(), Matchers.`is`(false))
        MatcherAssert.assertThat(Namespace.SPECIAL.talk(), Matchers.`is`(false))
    }

    @Test
    fun testTalkZero() {
        MatcherAssert.assertThat(Namespace.MAIN.talk(), Matchers.`is`(false))
    }

    @Test
    fun testTalkOdd() {
        MatcherAssert.assertThat(Namespace.TALK.talk(), Matchers.`is`(true))
    }
}

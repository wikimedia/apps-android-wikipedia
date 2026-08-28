package org.wikipedia.page

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite

@RunWith(RobolectricTestRunner::class)
class NamespaceTest {

    @Test
    fun testOf() {
        assertEquals(Namespace.SPECIAL, Namespace.of(Namespace.SPECIAL.code()))
    }

    @Test
    fun testFromLegacyStringMain() {
        assertEquals(Namespace.MAIN, Namespace.fromLegacyString(WikiSite.forLanguageCode("test"), null))
    }

    @Test
    fun testFromLegacyStringFile() {
        assertEquals(Namespace.FILE, Namespace.fromLegacyString(WikiSite.forLanguageCode("he"), "קובץ"))
    }

    @Test
    fun testFromLegacyStringSpecial() {
        assertEquals(Namespace.SPECIAL, Namespace.fromLegacyString(WikiSite.forLanguageCode("lez"), "Служебная"))
    }

    @Test
    fun testFromLegacyStringTalk() {
        assertEquals(Namespace.TALK, Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "Talk"))
        assertEquals(Namespace.TALK, Namespace.fromLegacyString(WikiSite.forLanguageCode("ru"), "Обсуждение"))
    }

    @Test
    fun testFromLegacyStringUser() {
        assertEquals(Namespace.USER, Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "User"))
        assertEquals(Namespace.USER, Namespace.fromLegacyString(WikiSite.forLanguageCode("af"), "Gebruiker"))
    }

    @Test
    fun testFromLegacyStringUserTalk() {
        assertEquals(Namespace.USER_TALK, Namespace.fromLegacyString(WikiSite.forLanguageCode("en"), "User talk"))
        assertEquals(Namespace.USER_TALK, Namespace.fromLegacyString(WikiSite.forLanguageCode("vi"), "Thảo luận Thành viên"))
    }

    @Test
    fun testCode() {
        assertEquals(0, Namespace.MAIN.code())
        assertEquals(1, Namespace.TALK.code())
    }

    @Test
    fun testSpecial() {
        assertTrue(Namespace.SPECIAL.special())
        assertFalse(Namespace.MAIN.special())
    }

    @Test
    fun testMain() {
        assertTrue(Namespace.MAIN.main())
        assertFalse(Namespace.TALK.main())
    }

    @Test
    fun testFile() {
        assertTrue(Namespace.FILE.file())
        assertFalse(Namespace.MAIN.file())
    }

    @Test
    fun testTalkNegative() {
        assertFalse(Namespace.MEDIA.talk())
        assertFalse(Namespace.SPECIAL.talk())
    }

    @Test
    fun testTalkZero() {
        assertFalse(Namespace.MAIN.talk())
    }

    @Test
    fun testTalkOdd() {
        assertTrue(Namespace.TALK.talk())
    }
}

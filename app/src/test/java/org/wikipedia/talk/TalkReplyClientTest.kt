package org.wikipedia.talk

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class TalkReplyClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testReplyRequiresHCaptcha() {
        enqueueFromFile("talk_page_reply_captcha.json")
        runBlocking {
            apiService.postTalkPageTopicReply("Talk:Foo", "c-Bar", "hi", "token")
        }.run {
            val captcha = result?.captcha
            assertTrue(captcha?.isHCaptcha == true)
            assertEquals("5d0c670e-a5f4-4258-ad16-1f42792c9c62", captcha?.siteKey)
            assertFalse(captcha?.forceShowCaptcha == true)
            assertEquals(0L, result?.newRevId)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testReplyRequiresForceShowHCaptcha() {
        enqueueFromFile("talk_page_reply_forceshow_captcha.json")
        runBlocking {
            apiService.postTalkPageTopicReply("Talk:Foo", "c-Bar", "hi", "token")
        }.run {
            val captcha = result?.captcha
            assertTrue(captcha?.isHCaptcha == true)
            assertEquals("45205f58-be1c-40f0-b286-07a4498ea3da", captcha?.siteKey)
            assertTrue(captcha?.forceShowCaptcha == true)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testReplySuccess() {
        enqueueFromFile("talk_page_reply_success.json")
        runBlocking {
            apiService.postTalkPageTopicReply("Talk:Foo", "c-Bar", "hi", "token")
        }.run {
            assertEquals(987654L, result?.newRevId)
            assertNull(result?.captcha)
        }
    }
}

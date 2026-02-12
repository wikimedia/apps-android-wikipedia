package org.wikipedia.descriptions

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.page.Namespace
import org.wikipedia.page.Page
import org.wikipedia.page.PageProperties
import org.wikipedia.page.PageTitle
import org.wikipedia.test.MockRetrofitTest
import java.util.regex.Pattern

class DescriptionEditClientTest : MockRetrofitTest() {
    @Test
    fun testEditLocalDescriptionWithRegex() {
        val text =
            "test test test test {{Short description|This is a description.}} foo foo {{Another template|12345}} foo foo"
        val newText = text.replaceFirst(
            DescriptionEditViewModel.TEMPLATE_PARSE_REGEX.toRegex(),
            "$1" + "New description." + "$3"
        )
        assertTrue(Pattern.compile(DescriptionEditViewModel.TEMPLATE_PARSE_REGEX).matcher(text).find())
        assertEquals(
            "test test test test {{Short description|New description.}} foo foo {{Another template|12345}} foo foo",
            newText
        )
    }

    @Test
    fun testRegexWithNoLocalDescription() {
        val text = "test test test test foo foo {{Another template|12345}} foo foo"
        assertFalse(Pattern.compile(DescriptionEditViewModel.TEMPLATE_PARSE_REGEX).matcher(text).find())
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestPostDescriptionSuccess() {
        enqueueFromFile("description_edit.json")
        runBlocking {
            requestPostDescription()
        }.run {
            assertEquals("Q123", entity?.id)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestPostLabelSuccess() {
        enqueueFromFile("label_edit.json")
        runBlocking {
            requestPostLabel()
        }.run {
            assertEquals("Q456", entity?.id)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestAbusefilterWarning() {
        enqueueFromFile("description_edit_abusefilter_warning.json")
        val expectedCode = "abusefilter-warning"
        val expectedMessage =
            "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Possible vandalism by adding badwords or similar trolling words"
        runBlocking {
            try {
                requestPostDescription()
            } catch (e: Exception) {
                assertTrue(testErrorWithExpectedCodeAndMessage(e, expectedCode, expectedMessage))
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestAbusefilterDisallowed() {
        enqueueFromFile("description_edit_abusefilter_disallowed.json")
        val expectedCode = "abusefilter-disallowed"
        val expectedMessage =
            "This action has been automatically identified as harmful, and therefore disallowed.\nIf you believe your action was constructive, please inform an administrator of what you were trying to do."
        runBlocking {
            try {
                requestPostDescription()
            } catch (e: Exception) {
                assertTrue(testErrorWithExpectedCodeAndMessage(e, expectedCode, expectedMessage))
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                requestPostDescription()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueueFromFile("description_edit_unknown_site.json")
        runBlocking {
            try {
                requestPostDescription()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                requestPostDescription()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    fun testIsEditAllowedSuccess() {
        val wiki = WikiSite.forLanguageCode("ru")
        val props = mockk<PageProperties>(relaxed = true) {
            every { wikiBaseItem } returns "Q123"
            every { canEdit } returns true
            every { descriptionSource } returns "central"
            every { namespace } returns Namespace.MAIN
        }
        val page = Page(PageTitle("Test", wiki), pageProperties = props)
        assertTrue(DescriptionEditUtil.isEditAllowed(page))
    }

    @Test
    fun testIsEditAllowedNoWikiBaseItem() {
        val wiki = WikiSite.forLanguageCode("ru")
        val props = mockk<PageProperties>(relaxed = true) {
            every { wikiBaseItem } returns null
            every { namespace } returns Namespace.MAIN
        }
        val page = Page(PageTitle("Test", wiki), pageProperties = props)
        assertFalse(DescriptionEditUtil.isEditAllowed(page))
    }

    private fun testErrorWithExpectedCodeAndMessage(caught: Exception, expectedCode: String, expectedMessage: String): Boolean {
        if (caught is MwException) {
            val error = caught.error
            return error.hasMessageName(expectedCode) && error.getMessageHtml(expectedCode) == expectedMessage
        } else {
            return false
        }
    }

    private suspend fun requestPostDescription(): EntityPostResponse {
        val pageTitle = PageTitle("foo", WikiSite.forLanguageCode("en"))
        return apiService.postDescriptionEdit(
            pageTitle.wikiSite.languageCode,
            pageTitle.wikiSite.languageCode, pageTitle.wikiSite.dbName(),
            pageTitle.prefixedText, "some new description", "summary", MOCK_EDIT_TOKEN, null
        )
    }

    private suspend fun requestPostLabel(): EntityPostResponse {
        val pageTitle = PageTitle("foo", WikiSite.forLanguageCode("en"))
        return apiService.postLabelEdit(
            pageTitle.wikiSite.languageCode,
            pageTitle.wikiSite.languageCode, pageTitle.wikiSite.dbName(),
            pageTitle.prefixedText, "some new label", "summary", MOCK_EDIT_TOKEN, null
        )
    }

    companion object {
        private const val MOCK_EDIT_TOKEN = "+\\"
    }
}

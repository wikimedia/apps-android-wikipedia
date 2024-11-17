package org.wikipedia.descriptions

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.mockito.Mockito
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
        MatcherAssert.assertThat(
            Pattern.compile(DescriptionEditViewModel.TEMPLATE_PARSE_REGEX).matcher(text).find(),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            newText,
            Matchers.`is`("test test test test {{Short description|New description.}} foo foo {{Another template|12345}} foo foo")
        )
    }

    @Test
    fun testRegexWithNoLocalDescription() {
        val text = "test test test test foo foo {{Another template|12345}} foo foo"
        MatcherAssert.assertThat(
            Pattern.compile(DescriptionEditViewModel.TEMPLATE_PARSE_REGEX).matcher(text).find(),
            Matchers.`is`(false)
        )
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestPostDescriptionSuccess() {
        enqueueFromFile("description_edit.json")
        runBlocking {
            requestPostDescription()
        }.run {
            MatcherAssert.assertThat(entity?.id, Matchers.`is`("Q123"))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestPostLabelSuccess() {
        enqueueFromFile("label_edit.json")
        runBlocking {
            requestPostLabel()
        }.run {
            MatcherAssert.assertThat(entity?.id, Matchers.`is`("Q456"))
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
                MatcherAssert.assertThat(testErrorWithExpectedCodeAndMessage(e, expectedCode, expectedMessage), Matchers.`is`(true))
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
                MatcherAssert.assertThat(testErrorWithExpectedCodeAndMessage(e, expectedCode, expectedMessage), Matchers.`is`(true))
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    fun testIsEditAllowedSuccess() {
        val wiki = WikiSite.forLanguageCode("ru")
        val props = Mockito.mock(PageProperties::class.java)
        Mockito.`when`(props.wikiBaseItem).thenReturn("Q123")
        Mockito.`when`(props.canEdit).thenReturn(true)
        Mockito.`when`(props.descriptionSource).thenReturn("central")
        Mockito.`when`(props.namespace).thenReturn(Namespace.MAIN)
        val page = Page(PageTitle("Test", wiki), pageProperties = props)
        MatcherAssert.assertThat(DescriptionEditUtil.isEditAllowed(page), Matchers.`is`(true))
    }

    @Test
    fun testIsEditAllowedNoWikiBaseItem() {
        val wiki = WikiSite.forLanguageCode("ru")
        val props = Mockito.mock(PageProperties::class.java)
        Mockito.`when`(props.wikiBaseItem).thenReturn(null)
        Mockito.`when`(props.namespace).thenReturn(Namespace.MAIN)
        val page = Page(PageTitle("Test", wiki), pageProperties = props)
        MatcherAssert.assertThat(DescriptionEditUtil.isEditAllowed(page), Matchers.`is`(false))
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

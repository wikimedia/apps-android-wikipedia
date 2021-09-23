package org.wikipedia.descriptions

import com.google.gson.stream.MalformedJsonException
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.TestObserver
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.mockito.Mockito
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.descriptions.DescriptionEditUtil.isEditAllowed
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
            DescriptionEditFragment.TEMPLATE_PARSE_REGEX.toRegex(),
            "$1" + "New description." + "$3"
        )
        MatcherAssert.assertThat(
            Pattern.compile(DescriptionEditFragment.TEMPLATE_PARSE_REGEX).matcher(text).find(),
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
            Pattern.compile(DescriptionEditFragment.TEMPLATE_PARSE_REGEX).matcher(text).find(),
            Matchers.`is`(false)
        )
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("description_edit.json")
        request().test().await()
            .assertComplete().assertNoErrors()
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestAbusefilterWarning() {
        enqueueFromFile("description_edit_abusefilter_warning.json")
        val expectedCode = "abusefilter-warning"
        val expectedMessage =
            "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Possible vandalism by adding badwords or similar trolling words"
        testErrorWithExpectedCodeAndMessage(request().test().await(), expectedCode, expectedMessage)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestAbusefilterDisallowed() {
        enqueueFromFile("description_edit_abusefilter_disallowed.json")
        val expectedCode = "abusefilter-disallowed"
        val expectedMessage =
            "This action has been automatically identified as harmful, and therefore disallowed.\nIf you believe your action was constructive, please inform an administrator of what you were trying to do."
        request()
        testErrorWithExpectedCodeAndMessage(request().test().await(), expectedCode, expectedMessage)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        request().test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueueFromFile("description_edit_unknown_site.json")
        request().test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        request().test().await()
            .assertError(MalformedJsonException::class.java)
    }

    @Test
    fun testIsEditAllowedSuccess() {
        val wiki = forLanguageCode("ru")
        val props = Mockito.mock(PageProperties::class.java)
        Mockito.`when`(props.wikiBaseItem).thenReturn("Q123")
        Mockito.`when`(props.canEdit).thenReturn(true)
        Mockito.`when`(props.descriptionSource).thenReturn("central")
        val page = Page(PageTitle("Test", wiki), emptyList(), props)
        MatcherAssert.assertThat(isEditAllowed(page), Matchers.`is`(true))
    }

    @Test
    fun testIsEditAllowedNoWikiBaseItem() {
        val wiki = forLanguageCode("ru")
        val props = Mockito.mock(PageProperties::class.java)
        Mockito.`when`(props.wikiBaseItem).thenReturn(null)
        val page = Page(PageTitle("Test", wiki), emptyList(), props)
        MatcherAssert.assertThat(isEditAllowed(page), Matchers.`is`(false))
    }

    private fun testErrorWithExpectedCodeAndMessage(
        observer: TestObserver<EntityPostResponse>,
        expectedCode: String,
        expectedMessage: String
    ) {
        observer.assertError { caught ->
            if (caught is MwException) {
                val error = caught.error
                return@assertError error.hasMessageName(expectedCode) && error.getMessageHtml(expectedCode) == expectedMessage
            } else {
                return@assertError false
            }
        }
    }

    private fun request(): Observable<EntityPostResponse> {
        val pageTitle = PageTitle("foo", forLanguageCode("en"))
        return apiService.postDescriptionEdit(
            pageTitle.wikiSite.languageCode,
            pageTitle.wikiSite.languageCode, pageTitle.wikiSite.dbName(),
            pageTitle.prefixedText, "some new description", "summary", MOCK_EDIT_TOKEN, null
        )
    }

    companion object {
        private const val MOCK_EDIT_TOKEN = "+\\"
    }
}

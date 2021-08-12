package org.wikipedia.descriptions;

import androidx.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.wikidata.EntityPostResponse;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockRetrofitTest;

import java.util.Collections;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DescriptionEditClientTest extends MockRetrofitTest {
    private static final String MOCK_EDIT_TOKEN = "+\\";

    @Test public void testEditLocalDescriptionWithRegex() {
        String text = "test test test test {{Short description|This is a description.}} foo foo {{Another template|12345}} foo foo";
        String newText = text.replaceFirst(DescriptionEditFragment.TEMPLATE_PARSE_REGEX, "$1" + "New description." + "$3");
        assertThat(Pattern.compile(DescriptionEditFragment.TEMPLATE_PARSE_REGEX).matcher(text).find(), is(true));
        assertThat(newText, is("test test test test {{Short description|New description.}} foo foo {{Another template|12345}} foo foo"));
    }

    @Test public void testRegexWithNoLocalDescription() {
        String text = "test test test test foo foo {{Another template|12345}} foo foo";
        assertThat(Pattern.compile(DescriptionEditFragment.TEMPLATE_PARSE_REGEX).matcher(text).find(), is(false));
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("description_edit.json");
        request().test().await()
                .assertComplete().assertNoErrors();
    }

    @Test public void testRequestAbusefilterWarning() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_warning.json");

        String expectedCode = "abusefilter-warning";
        String expectedMessage = "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Possible vandalism by adding badwords or similar trolling words";

        testErrorWithExpectedCodeAndMessage(request().test().await(), expectedCode, expectedMessage);
    }

    @Test public void testRequestAbusefilterDisallowed() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_disallowed.json");

        String expectedCode = "abusefilter-disallowed";
        String expectedMessage = "This action has been automatically identified as harmful, and therefore disallowed.\nIf you believe your action was constructive, please inform an administrator of what you were trying to do.";

        request();
        testErrorWithExpectedCodeAndMessage(request().test().await(), expectedCode, expectedMessage);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");


        request().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueueFromFile("description_edit_unknown_site.json");
        request().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        request().test().await()
                .assertError(MalformedJsonException.class);
    }

    @Test public void testIsEditAllowedSuccess() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn("Q123");
        when(props.canEdit()).thenReturn(true);
        when(props.getDescriptionSource()).thenReturn("central");
        Page page = new Page(new PageTitle("Test", wiki),
                Collections.emptyList(), props);

        assertThat(DescriptionEditUtil.isEditAllowed(page), is(true));
    }

    @Test public void testIsEditAllowedNoWikiBaseItem() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn(null);
        Page page = new Page(new PageTitle("Test", wiki),
                Collections.emptyList(), props);

        assertThat(DescriptionEditUtil.isEditAllowed(page), is(false));
    }

    private void testErrorWithExpectedCodeAndMessage(@NonNull TestObserver<EntityPostResponse> observer,
                                                     @NonNull String expectedCode,
                                                     @NonNull String expectedMessage) {
        observer.assertError(caught -> {
            if (caught instanceof MwException) {
                MwServiceError error = ((MwException) caught).getError();
                return error.hasMessageName(expectedCode) && error.getMessageHtml(expectedCode).equals(expectedMessage);
            } else {
                return false;
            }
        });
    }

    private Observable<EntityPostResponse> request() {
        final PageTitle pageTitle = new PageTitle("foo", WikiSite.forLanguageCode("en"));
        return getApiService().postDescriptionEdit(pageTitle.getWikiSite().languageCode(),
                pageTitle.getWikiSite().languageCode(), pageTitle.getWikiSite().dbName(),
                pageTitle.getPrefixedText(), "some new description", "summary", MOCK_EDIT_TOKEN, null);
    }
}

package org.wikipedia.descriptions;

import androidx.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.test.MockRetrofitTest;

import java.util.Collections;

import io.reactivex.observers.TestObserver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DescriptionEditClientTest extends MockRetrofitTest {
    private static final String MOCK_EDIT_TOKEN = "+\\";

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("description_edit.json");

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        observer.assertComplete().assertNoErrors();
    }

    @Test public void testRequestAbusefilterWarning() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_warning.json");

        String expectedCode = "abusefilter-warning";
        String expectedMessage = "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Possible vandalism by adding badwords or similar trolling words";

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        testErrorWithExpectedCodeAndMessage(observer, expectedCode, expectedMessage);
    }

    @Test public void testRequestAbusefilterDisallowed() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_disallowed.json");

        String expectedCode = "abusefilter-disallowed";
        String expectedMessage = "This action has been automatically identified as harmful, and therefore disallowed.\nIf you believe your action was constructive, please inform an administrator of what you were trying to do.";

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        testErrorWithExpectedCodeAndMessage(observer, expectedCode, expectedMessage);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueueFromFile("description_edit_unknown_site.json");

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();

        TestObserver<MwPostResponse> observer = new TestObserver<>();
        request(observer);
        observer.assertError(MalformedJsonException.class);
    }

    @Test public void testIsEditAllowedSuccess() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn("Q123");
        when(props.canEdit()).thenReturn(true);
        when(props.getDescriptionSource()).thenReturn("central");
        Page page = new Page(new PageTitle("Test", wiki, null, null, props),
                Collections.<Section>emptyList(), props);

        assertThat(DescriptionEditUtil.isEditAllowed(page), is(true));
    }

    @Test public void testIsEditAllowedNoWikiBaseItem() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn(null);
        Page page = new Page(new PageTitle("Test", wiki, null, null, props),
                Collections.<Section>emptyList(), props);

        assertThat(DescriptionEditUtil.isEditAllowed(page), is(false));
    }

    private void testErrorWithExpectedCodeAndMessage(@NonNull TestObserver<MwPostResponse> observer,
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


    private void request(@NonNull TestObserver<MwPostResponse> observer) {
        final PageTitle pageTitle = new PageTitle("foo", WikiSite.forLanguageCode("en"));
        getApiService().postDescriptionEdit(pageTitle.getWikiSite().languageCode(),
                pageTitle.getWikiSite().languageCode(), pageTitle.getWikiSite().dbName(),
                pageTitle.getPrefixedText(), "some new description", "summary", MOCK_EDIT_TOKEN, null)
                .subscribe(observer);
    }
}

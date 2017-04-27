package org.wikipedia.descriptions;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.descriptions.DescriptionEditClient.Callback;
import org.wikipedia.descriptions.DescriptionEditClient.Service;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.test.MockWebServerTest;

import java.util.Collections;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DescriptionEditClientTest extends MockWebServerTest {
    private static final String MOCK_EDIT_TOKEN = "+\\";

    @NonNull private final DescriptionEditClient subject = new DescriptionEditClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("description_edit.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestAbusefilterWarning() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_warning.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackAbusefilter(call, cb, "abusefilter-warning",
                "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Possible vandalism by adding badwords or similar trolling words");
    }

    @Test public void testRequestAbusefilterDisallowed() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_disallowed.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackAbusefilter(call, cb, "abusefilter-disallowed",
                "This action has been automatically identified as harmful, and therefore disallowed.\nIf you believe your action was constructive, please inform an administrator of what you were trying to do.");
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueueFromFile("description_edit_unknown_site.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    @Test public void testIsEditAllowedSuccess() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn("Q123");
        when(props.canEdit()).thenReturn(true);
        Page page = new Page(new PageTitle("Test", wiki, null, null, props),
                Collections.<Section>emptyList(), props);

        assertThat(DescriptionEditClient.isEditAllowed(page), is(true));
    }

    @Test public void testIsEditAllowedNoWikiBaseItem() {
        WikiSite wiki = WikiSite.forLanguageCode("ru");
        PageProperties props = mock(PageProperties.class);
        when(props.getWikiBaseItem()).thenReturn(null);
        Page page = new Page(new PageTitle("Test", wiki, null, null, props),
                Collections.<Section>emptyList(), props);

        assertThat(DescriptionEditClient.isEditAllowed(page), is(false));
    }

    private void assertCallbackSuccess(@NonNull Call<DescriptionEdit> call,
                                       @NonNull Callback cb) {
        verify(cb).success(eq(call));
        //noinspection unchecked
        verify(cb, never()).abusefilter(any(Call.class), any(String.class), any(String.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackAbusefilter(@NonNull Call<DescriptionEdit> call,
                                           @NonNull Callback cb,
                                           String expectedCode,
                                           String expectedMessage) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class));
        verify(cb).abusefilter(eq(call), eq(expectedCode), eq(expectedMessage));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<DescriptionEdit> call,
                                       @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class));
        //noinspection unchecked
        verify(cb, never()).abusefilter(any(Call.class), any(String.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<DescriptionEdit> request(@NonNull Callback cb) {
        final PageTitle pageTitle = new PageTitle("foo", WikiSite.forLanguageCode("en"));
        return subject.request(service(Service.class), pageTitle, "some new description",
                MOCK_EDIT_TOKEN, false, cb);
    }
}

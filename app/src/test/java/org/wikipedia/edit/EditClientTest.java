package org.wikipedia.edit;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EditClientTest extends MockWebServerTest {
    private EditClient subject = new EditClient();

    @Test @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessHasResults() throws Throwable {
        EditSuccessResult expected = new EditSuccessResult(761350490);
        enqueueFromFile("edit_result_success.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseAbuseFilter() throws Throwable {
        EditAbuseFilterResult expected = new EditAbuseFilterResult("abusefilter-disallowed",
                "Hit AbuseFilter: Editing user page by anonymous user",
                "<b>Warning:</b> This action has been automatically identified as harmful.\nUnconstructive edits will be quickly reverted,\nand egregious or repeated unconstructive editing will result in your account or IP address being blocked.\nIf you believe this action to be constructive, you may submit it again to confirm it.\nA brief description of the abuse rule which your action matched is: Editing user page by anonymous user");
        enqueueFromFile("edit_abuse_filter_result.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseSpamBlacklist() throws Throwable {
        EditSpamBlacklistResult expected = new EditSpamBlacklistResult("s-e-x");
        enqueueFromFile("edit_result_spam_blacklist.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestResponseCaptcha() throws Throwable {
        CaptchaResult expected = new CaptchaResult("547159230");
        enqueueFromFile("edit_result_captcha.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseAssertUserFailed() throws Throwable {
        enqueueFromFile("api_error_assert_user_failed.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, true);

        server().takeRequest();
        assertCallbackFailure(call, cb, UserNotLoggedInException.class);
    }

    @Test public void testRequestResponseGenericApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("(-(-_(-_-)_-)-)");

        EditClient.Callback cb = mock(EditClient.Callback.class);
        Call<Edit> call = request(cb, false);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<Edit> call,
                                       @NonNull EditClient.Callback cb,
                                       @NonNull EditResult expected) {
        verify(cb).success(eq(call), eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<Edit> call,
                                       @NonNull EditClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(EditResult.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<Edit> request(@NonNull EditClient.Callback cb, boolean loggedIn) {
        PageTitle title = new PageTitle(null, "TEST", WikiSite.forLanguageCode("test"));
        return subject.request(service(EditClient.Service.class), title, 0, "new text", "token",
                "summary", loggedIn, "captchaId", "captchaSubmission", cb);
    }
}

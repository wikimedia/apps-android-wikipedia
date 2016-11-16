package org.wikipedia.edit;


import android.support.annotation.NonNull;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.edit.EditClient.Callback;
import org.wikipedia.edit.EditClient.Service;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EditUnitTest extends MockWebServerTest {
    @NonNull private EditClient client = new EditClient();

    @Test public void testAbuseFilterResult() throws Throwable {
        enqueueFromFile("edit_abuse_filter_result.json");

        Callback cb = mock(Callback.class);
        Call<Edit> call = request(cb);

        server().takeRequest();
        assertAbuseFilterEditResult(call, cb);
    }

    @Test public void testBadToken() throws Throwable {
        enqueueFromFile("edit_error_bad_token.json");

        Callback cb = mock(Callback.class);
        Call<Edit> call = request(cb);

        server().takeRequest();
        assertExpectedEditError(call, cb, "Invalid token");
    }

    @Test public void testRequestUserNotLoggedIn() throws Throwable {
        enqueueFromFile("edit_user_not_logged_in.json");

        Callback cb = mock(Callback.class);
        Call<Edit> call = request(cb);

        server().takeRequest();
        assertExpectedEditError(call, cb, "User not logged in.");
    }

    @NonNull private Call<Edit> request(@NonNull Callback cb) {
        return client.request(service(Service.class), new PageTitle("FAKE TITLE",
                WikiSite.forLanguageCode("test")), 0, "FAKE EDIT TEXT", "+/", "FAKE SUMMARY", false,
                null, null, cb);
    }

    private void assertAbuseFilterEditResult(@NonNull Call<Edit> call,
                                       @NonNull Callback cb) {
        verify(cb).success(eq(call), isA(EditAbuseFilterResult.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertExpectedEditError(@NonNull Call<Edit> call,
                                       @NonNull Callback cb,
                                       @NonNull String expectedCode) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(EditSuccessResult.class));
        verify(cb).failure(eq(call), captor.capture());
        Throwable t = captor.getValue();
        assertThat(t.getMessage(), is(expectedCode));
    }
}

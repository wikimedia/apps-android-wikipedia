package org.wikipedia.login;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.rxjava3.core.Observable;

public class UserExtendedInfoClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_extended_info.json");
        final int id = 24531888;
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getQuery().getUserInfo().getId() == id
                        && result.getQuery().getUserResponse("USER").name().equals("USER"));
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getUserInfo();
    }
}

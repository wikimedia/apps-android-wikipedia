package org.wikipedia.createaccount;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;

public class CreateAccountInfoClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("create_account_info.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> {
                    String token = response.getQuery().getCreateAccountToken();
                    String captchaId = response.getQuery().getCaptchaId();

                    return token.equals("5d78e6a823be0901eeae9f6486f752da59123760+\\")
                            && captchaId.equals("272460457");
                });
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getAuthManagerInfo();
    }
}

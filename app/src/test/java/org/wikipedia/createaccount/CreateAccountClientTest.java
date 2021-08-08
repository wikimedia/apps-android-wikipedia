package org.wikipedia.createaccount;

import org.junit.Test;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.mwapi.CreateAccountResponse;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;

public class CreateAccountClientTest extends MockRetrofitTest {

    private Observable<CreateAccountResponse> getObservable() {
        return getApiService().postCreateAccount("user", "pass", "pass", "token", Service.WIKIPEDIA_URL, null, null, null);
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("create_account_success.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getStatus().equals("PASS")
                        && result.getUser().equals("Farb0nucci"));
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueueFromFile("create_account_failure.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getStatus().equals("FAIL"));
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
}

package org.wikipedia.createaccount;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class CreateAccountInfoClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("create_account_info.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getAuthManagerInfo().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(response -> {
                    String token = response.query().createAccountToken();
                    String captchaId = response.query().captchaId();

                    return token.equals("5d78e6a823be0901eeae9f6486f752da59123760+\\")
                            && captchaId.equals("272460457");
                });
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getAuthManagerInfo().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("┏━┓ ︵  /(^.^/)");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getAuthManagerInfo().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}

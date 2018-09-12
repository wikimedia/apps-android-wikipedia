package org.wikipedia.createaccount;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.mwapi.CreateAccountResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class CreateAccountClientTest extends MockRetrofitTest {

    private Observable<CreateAccountResponse> getObservable() {
        return getApiService().postCreateAccount("user", "pass", "pass", "token", Service.WIKIPEDIA_URL, null, null, null);
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("create_account_success.json");
        TestObserver<CreateAccountResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.status().equals("PASS")
                        && result.user().equals("Farb0nucci"));
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueueFromFile("create_account_failure.json");
        TestObserver<CreateAccountResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.status().equals("FAIL"));
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();
        TestObserver<CreateAccountResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("┏━┓ ︵  /(^.^/)");
        TestObserver<CreateAccountResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}

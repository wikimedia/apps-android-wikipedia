package org.wikipedia.login;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class UserExtendedInfoClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_extended_info.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getUserInfo("USER").subscribe(observer);

        final int id = 24531888;
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.query().userInfo().id() == id
                        && result.query().getUserResponse("USER").name().equals("USER"));
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getUserInfo("USER").subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("┏━┓ ︵  /(^.^/)");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getApiService().getUserInfo("USER").subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}

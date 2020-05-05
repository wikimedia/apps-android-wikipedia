package org.wikipedia.login;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class UserExtendedInfoClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_extended_info.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        final int id = 24531888;
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.query().userInfo().getId() == id
                        && result.query().getUserResponse("USER").getName().equals("USER"));
    }

    @Test public void testRequestResponse404() {
        enqueue404();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getUserInfo();
    }
}

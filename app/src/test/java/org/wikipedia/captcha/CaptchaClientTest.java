package org.wikipedia.captcha;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;

public class CaptchaClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("captcha.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getCaptchaId().equals("1572672319"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    private Observable<CaptchaResult> getObservable() {
        return getApiService().getNewCaptcha()
                        .map(response -> new CaptchaResult(response.captchaId()));
    }
}

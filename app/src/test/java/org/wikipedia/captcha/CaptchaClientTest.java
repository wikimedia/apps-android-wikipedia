package org.wikipedia.captcha;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class CaptchaClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("captcha.json");
        TestObserver<CaptchaResult> observer = new TestObserver<>();

        getApiService().getNewCaptcha()
                .map(response -> new CaptchaResult(response.captchaId()))
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getCaptchaId().equals("1572672319"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<CaptchaResult> observer = new TestObserver<>();

        getApiService().getNewCaptcha()
                .map(response -> new CaptchaResult(response.captchaId()))
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        TestObserver<CaptchaResult> observer = new TestObserver<>();

        getApiService().getNewCaptcha()
                .map(response -> new CaptchaResult(response.captchaId()))
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");
        TestObserver<CaptchaResult> observer = new TestObserver<>();

        getApiService().getNewCaptcha()
                .map(response -> new CaptchaResult(response.captchaId()))
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

}

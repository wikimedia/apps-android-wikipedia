package org.wikipedia.zero;

import android.graphics.Color;
import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class ZeroConfigClientTest extends MockRetrofitTest {
    @NonNull private static String USER_AGENT = WikipediaApp.getInstance().getUserAgent();

    @Test public void testRequestEligible() throws Throwable {
        enqueueFromFile("wikipedia_zero_test_eligible.json");
        TestObserver<ZeroConfig> observer = new TestObserver<>();

        getApiService().getZeroConfig(USER_AGENT)
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getMessage().equals("Overstay your stay!")
                        && result.getBackground() == Color.CYAN
                        && result.getForeground() == Color.WHITE
                        && result.getExitTitle().equals("You are leaving free Wikipedia service")
                        && result.getExitWarning().equals("Data charges will be applied to your account")
                        && result.getPartnerInfoText().equals("Learn more at zero.wikimedia.org")
                        && result.getBannerUrl().equals("https://zero.wikimedia.org"));
    }

    @Test public void testRequestIneligible() throws Throwable {
        enqueueEmptyJson();
        TestObserver<ZeroConfig> observer = new TestObserver<>();

        getApiService().getZeroConfig(USER_AGENT)
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> !result.isEligible());
    }

    @Test public void testRequestMalformed() throws Throwable {
        server().enqueue("'");
        TestObserver<ZeroConfig> observer = new TestObserver<>();

        getApiService().getZeroConfig(USER_AGENT)
                .subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();
        enqueueFromFile("api_error.json");
        TestObserver<ZeroConfig> observer = new TestObserver<>();

        getApiService().getZeroConfig(USER_AGENT)
                .subscribe(observer);

        observer.assertError(Exception.class);
    }
}

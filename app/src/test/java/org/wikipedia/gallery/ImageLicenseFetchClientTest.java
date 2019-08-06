package org.wikipedia.gallery;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class ImageLicenseFetchClientTest extends MockRetrofitTest {
    private static final WikiSite WIKISITE_TEST = WikiSite.forLanguageCode("test");
    private static final PageTitle PAGE_TITLE_MARK_SELBY =
            new PageTitle("File:Mark_Selby_at_Snooker_German_Masters_(DerHexer)_2015-02-04_02.jpg",
                          WIKISITE_TEST);

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("image_license.json");
        TestObserver<ImageLicense> observer = new TestObserver<>();

        getApiService().getImageExtMetadata(PAGE_TITLE_MARK_SELBY.getPrefixedText())
                .map(response -> {
                    // noinspection ConstantConditions
                    MwQueryPage page = response.query().pages().get(0);
                    return page.imageInfo() != null && page.imageInfo().getMetadata() != null
                            ? new ImageLicense(page.imageInfo().getMetadata())
                            : new ImageLicense();
                })
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getLicenseName().equals("cc-by-sa-4.0")
                        && result.getLicenseShortName().equals("CC BY-SA 4.0")
                        && result.getLicenseUrl().equals("http://creativecommons.org/licenses/by-sa/4.0"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<ImageLicense> observer = new TestObserver<>();

        getApiService().getImageExtMetadata(PAGE_TITLE_MARK_SELBY.getPrefixedText())
                .map(response -> new ImageLicense())
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() {
        enqueue404();
        TestObserver<ImageLicense> observer = new TestObserver<>();

        getApiService().getImageExtMetadata(PAGE_TITLE_MARK_SELBY.getPrefixedText())
                .map(response -> new ImageLicense())
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<ImageLicense> observer = new TestObserver<>();

        getApiService().getImageExtMetadata(PAGE_TITLE_MARK_SELBY.getPrefixedText())
                .map(response -> new ImageLicense())
                .subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}

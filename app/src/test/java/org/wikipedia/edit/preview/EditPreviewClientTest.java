package org.wikipedia.edit.preview;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.observers.TestObserver;

public class EditPreviewClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        String expected = "<div class=\"mf-section-0\" id=\"mf-section-0\"><p>\\o/\\n\\ntest12\\n\\n3</p>\n\n\n\n\n</div>";

        enqueueFromFile("edit_preview.json");
        TestObserver<EditPreview> observer = new TestObserver<>();

        getApiService().postEditPreview("User:Mhollo/sandbox", "wikitext of change")
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(response -> response.result().equals(expected));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<EditPreview> observer = new TestObserver<>();

        getApiService().postEditPreview("User:Mhollo/sandbox", "wikitext of change")
                .subscribe(observer);

        observer.assertError(MwException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("(-(-_(-_-)_-)-)");
        TestObserver<EditPreview> observer = new TestObserver<>();

        getApiService().postEditPreview("User:Mhollo/sandbox", "wikitext of change")
                .subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}

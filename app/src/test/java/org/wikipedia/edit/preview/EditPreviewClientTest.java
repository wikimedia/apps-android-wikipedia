package org.wikipedia.edit.preview;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.rxjava3.core.Observable;

public class EditPreviewClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        String expected = "<div class=\"mf-section-0\" id=\"mf-section-0\"><p>\\o/\\n\\ntest12\\n\\n3</p>\n\n\n\n\n</div>";

        enqueueFromFile("edit_preview.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> response.result().equals(expected));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(MwException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    private Observable<EditPreview> getObservable() {
        return getApiService().postEditPreview("User:Mhollo/sandbox", "wikitext of change");
    }
}

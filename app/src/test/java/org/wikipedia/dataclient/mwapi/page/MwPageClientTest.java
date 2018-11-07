package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.page.BasePageClientTest;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;

import io.reactivex.observers.TestObserver;
import retrofit2.Response;

public class MwPageClientTest extends BasePageClientTest {
    private PageClient subject;

    @Before public void setUp() throws Throwable {
        super.setUp();
        subject = new MwPageClient();
    }

    @Test public void testLeadThumbnailWidth() throws Throwable {

        TestObserver<Response<PageLead>> observer = new TestObserver<>();
        subject.lead(wikiSite(), null, null, null, "test", 10).subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.raw().request().url().toString().contains("10"));
    }

    @NonNull @Override protected PageClient subject() {
        return subject;
    }
}

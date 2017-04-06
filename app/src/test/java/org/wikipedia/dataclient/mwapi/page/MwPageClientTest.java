package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.page.BasePageClientTest;
import org.wikipedia.dataclient.page.PageClient;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class MwPageClientTest extends BasePageClientTest {
    private PageClient subject;

    @Before public void setUp() throws Throwable {
        super.setUp();
        subject = new MwPageClient(service(MwPageService.class));
    }

    @Test public void testLeadThumbnailWidth() throws Throwable {
        Call<?> call = subject.lead(null, PageClient.CacheOption.CACHE, "", 10, false);
        assertThat(call.request().url().toString(), containsString("10"));
    }

    @NonNull @Override protected PageClient subject() {
        return subject;
    }
}

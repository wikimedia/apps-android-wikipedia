package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;

import org.junit.Test;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import retrofit2.Call;

import static org.junit.Assert.assertNotNull;

@SmallTest
public class FetchSectionWikitextTest {
    @Test public void testPageFetch() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("test");
        PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", wiki);
        int sectionId = 2;
        final TestLatch latch = new TestLatch();

        new WikitextClient().request(wiki, title, sectionId, new WikitextClient.Callback() {
            @Override
            public void success(@NonNull Call<Wikitext> call, @NonNull String wikitext) {
                assertNotNull(wikitext);
                latch.countDown();
            }

            @Override
            public void failure(@NonNull Call<Wikitext> call, @NonNull Throwable caught) {
                throw new RuntimeException(caught);
            }
        });
        latch.await();
    }
}

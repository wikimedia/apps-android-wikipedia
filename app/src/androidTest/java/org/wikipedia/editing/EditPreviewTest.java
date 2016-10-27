package org.wikipedia.editing;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import retrofit2.Call;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

@SmallTest
public class EditPreviewTest {
    @Test
    public void testPreview() throws Throwable {
        final WikiSite wiki = WikiSite.forLanguageCode("test");
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", wiki);
        final long randomTime = System.currentTimeMillis();
        final String wikiText = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + randomTime;

        final TestLatch latch = new TestLatch();

        new EditPreviewClient().request(wiki, title, wikiText,
                new EditPreviewClient.Callback() {
                    @Override
                    public void success(@NonNull Call<EditPreview> call, @NonNull String preview) {
                        assertThat(preview, containsString(String.valueOf(randomTime)));
                        latch.countDown();
                    }

                    @Override
                    public void failure(@NonNull Call<EditPreview> call, @NonNull Throwable caught) {
                        throw new RuntimeException(caught);
                    }
                });
        latch.await();
    }
}

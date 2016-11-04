package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import retrofit2.Call;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@SmallTest
public class EditPreviewTest {
    private static WikiSite TESTWIKI = WikiSite.forLanguageCode("test");

    @Test
    public void testPreview() throws Throwable {
        PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", TESTWIKI);
        final long randomTime = System.currentTimeMillis();
        String wikiText = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + randomTime;

        final TestLatch latch = new TestLatch();

        new EditPreviewClient().request(TESTWIKI, title, wikiText,
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

    @Test
    public void testErrorResponse() throws Throwable {
        PageTitle title = new PageTitle(null, "#[]", TESTWIKI);
        String wikiText = "foo";

        final TestLatch latch = new TestLatch();

        new EditPreviewClient().request(TESTWIKI, title, wikiText,
                new EditPreviewClient.Callback() {
                    @Override
                    public void success(@NonNull Call<EditPreview> call, @NonNull String preview) {
                        throw new RuntimeException("This should generate an error response!");
                    }

                    @Override
                    public void failure(@NonNull Call<EditPreview> call, @NonNull Throwable caught) {
                        assertNotNull(caught.getMessage());
                        latch.countDown();
                    }
                });
        latch.await();
    }
}

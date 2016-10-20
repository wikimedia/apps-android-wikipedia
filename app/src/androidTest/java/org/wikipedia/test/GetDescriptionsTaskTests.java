package org.wikipedia.test;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.wikidata.GetDescriptionsTask;

import android.support.test.filters.SmallTest;
import android.test.ActivityUnitTestCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests retrieval of Wikidata descriptions through enwiki.
 */
@SmallTest
public class GetDescriptionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200_000;
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    public GetDescriptionsTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testOneTitle() throws Throwable {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("Test", WIKI)}
        );
    }

    public void testThreeTitles() throws Throwable {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("SAT", WIKI),
                new PageTitle("Miller–Rabin primality test", WIKI),
                new PageTitle("Radiocarbon dating", WIKI)
        });
    }

    private void getWikidataDescriptions(final PageTitle[] ids) throws Throwable {
        final List<PageTitle> idList = new ArrayList<>(Arrays.asList(ids));
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new GetDescriptionsTask(app.getAPIForSite(WIKI), WIKI, idList) {
                    @Override
                    public void onFinish(Map<PageTitle, Void> descriptionsMap) {
                        assertNotNull(descriptionsMap);
                        assertEquals(descriptionsMap.size(), idList.size());
                        for (PageTitle title : idList) {
                            assertNotNull(title.getDescription());
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
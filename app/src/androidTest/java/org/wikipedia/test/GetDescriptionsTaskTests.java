package org.wikipedia.test;

import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.wikidata.GetDescriptionsTask;
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
public class GetDescriptionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200000;
    private static final Site SITE = Site.forLanguageCode("en");

    public GetDescriptionsTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testOneTitle() throws Throwable {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("Test", SITE)}
        );
    }

    public void testThreeTitles() throws Throwable {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("SAT", SITE),
                new PageTitle("Miller–Rabin primality test", SITE),
                new PageTitle("Radiocarbon dating", SITE)
        });
    }

    void getWikidataDescriptions(final PageTitle[] ids) throws Throwable {
        final List<PageTitle> idList = new ArrayList<>(Arrays.asList(ids));
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new GetDescriptionsTask(app.getAPIForSite(SITE), SITE, idList) {
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


package org.wikipedia.test;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.wikidata.WikidataDescriptionsTask;
import org.wikipedia.wikidata.WikidataSite;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests retrieval of Wikidata descriptions. Talks to wikidata.org.
 */
public class WikidataDescriptionsTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 200000;
    private static final Site SITE = new WikidataSite();

    public WikidataDescriptionsTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testOneID() throws Throwable {
        getWikidataDescriptions(new String[] {"Q42"});
    }

    public void testThreeIDs() throws Throwable {
        getWikidataDescriptions(new String[] {"Q1", "Q2", "Q3"});
    }

    public void getWikidataDescriptions(final String[] ids) throws Throwable {
        final ArrayList<String> idList = new ArrayList<String>(Arrays.asList(ids));
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
                new WikidataDescriptionsTask(app.getAPIForSite(SITE), "de", idList) {
                    @Override
                    public void onFinish(Map<String, String> descriptionsMap) {
                        assertNotNull(descriptionsMap);
                        assertEquals(descriptionsMap.size(), idList.size());
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}


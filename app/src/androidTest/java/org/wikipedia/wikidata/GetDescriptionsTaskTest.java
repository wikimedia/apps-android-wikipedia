package org.wikipedia.wikidata;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.wikipedia.test.TestUtil.runOnMainSync;

/**
 * Tests retrieval of Wikidata descriptions through enwiki.
 */
@SmallTest
public class GetDescriptionsTaskTest {
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    @Test public void testOneTitle() throws Throwable {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("Test", WIKI)}
        );
    }

    @Test public void testThreeTitles() {
        getWikidataDescriptions(new PageTitle[] {
                new PageTitle("SAT", WIKI),
                new PageTitle("Miller–Rabin primality test", WIKI),
                new PageTitle("Radiocarbon dating", WIKI)
        });
    }

    private void getWikidataDescriptions(final PageTitle[] ids) {
        final List<PageTitle> idList = new ArrayList<>(Arrays.asList(ids));
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WikipediaApp app = WikipediaApp.getInstance();
                new GetDescriptionsTask(app.getAPIForSite(WIKI), WIKI, idList) {
                    @Override
                    public void onFinish(Map<PageTitle, Void> descriptionsMap) {
                        assertThat(descriptionsMap, notNullValue());
                        assertThat(descriptionsMap.size(), is(idList.size()));
                        for (PageTitle title : idList) {
                            assertThat(title.getDescription(), notNullValue());
                        }
                        latch.countDown();
                    }
                }.execute();
            }
        });
        latch.await();
    }
}

package org.wikipedia.feed.featured;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.FeedPageSummary;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.test.TestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(TestRunner.class)
public class FeaturedArticleCardTest {
    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private FeedPageSummary content;

    @Before public void setUp() throws Throwable {
        String json = TestFileUtil.readRawFile("featured_2016_11_07.json");
        content = GsonUnmarshaller.unmarshal(FeedPageSummary.class, json);
    }

    @Test public void testTitleNormalization() throws Throwable {
        FeaturedArticleCard tfaCard = new FeaturedArticleCard(content, 0, TEST);
        assertThat(tfaCard.title(), not(containsString("_")));
    }
}

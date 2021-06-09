package org.wikipedia.feed.topread;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestFileUtil;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(RobolectricTestRunner.class)
public class TopReadItemCardTest {
    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private TopRead content;

    @Before public void setUp() throws Throwable {
        String json = TestFileUtil.readRawFile("mostread_2016_11_07.json");
        content = GsonUnmarshaller.unmarshal(TopRead.class, json);
    }

    @Test public void testTitleNormalization() {
        List<TopReadItemCard> topReadItemCards = TopReadListCard.toItems(content.articles(), TEST);
        for (TopReadItemCard topReadItemCard : topReadItemCards) {
            assertThat(topReadItemCard.title(), not(containsString("_")));
        }
    }
}

package org.wikipedia.feed.mostread;

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
public class MostReadItemCardTest {
    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private MostReadArticles content;

    @Before public void setUp() throws Throwable {
        String json = TestFileUtil.readRawFile("mostread_2016_11_07.json");
        content = GsonUnmarshaller.unmarshal(MostReadArticles.class, json);
    }

    @Test public void testTitleNormalization() throws Throwable {
        List<MostReadItemCard> mostReadItemCards = MostReadListCard.toItems(content.articles(), TEST);
        for (MostReadItemCard mostReadItemCard : mostReadItemCards) {
            assertThat(mostReadItemCard.title(), not(containsString("_")));
        }
    }
}

package org.wikipedia.feed.news;

import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.test.TestFileUtil;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NewsLinkCardTest {
    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private List<NewsItem> content;

    @Before public void setUp() throws Throwable {
        String json = TestFileUtil.readRawFile("news_2016_11_07.json");
        TypeToken<List<NewsItem>> typeToken = new TypeToken<List<NewsItem>>(){};
        content = GsonUtil.getDefaultGson().fromJson(json, typeToken.getType());
    }
}

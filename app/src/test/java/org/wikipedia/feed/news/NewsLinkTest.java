package org.wikipedia.feed.news;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.MoshiUtil;
import org.wikipedia.test.TestFileUtil;

import java.lang.reflect.Type;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(RobolectricTestRunner.class)
public class NewsLinkTest {
    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private List<NewsItem> content;

    @Before public void setUp() throws Throwable {
        final String json = TestFileUtil.readRawFile("news_2016_11_07.json");
        final Type type = Types.newParameterizedType(List.class, NewsItem.class);
        final JsonAdapter<List<NewsItem>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        content = adapter.fromJson(json);
    }

    @Test
    public void testTitleNormalization() {
        for (NewsItem newsItem : content) {
            for (PageSummary link : newsItem.getLinks()) {
                assertThat(new NewsLinkCard(link, TEST).title(), not(containsString("_")));
            }
        }
    }
}

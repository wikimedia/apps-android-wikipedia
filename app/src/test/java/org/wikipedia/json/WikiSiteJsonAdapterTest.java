package org.wikipedia.json;

import com.squareup.moshi.JsonAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(RobolectricTestRunner.class) public class WikiSiteJsonAdapterTest {
    @Test public void testWriteRead() throws IOException {
        WikiSite wiki = WikiSite.forLanguageCode("test");
        final JsonAdapter<WikiSite> adapter = MoshiUtil.getDefaultMoshi().adapter(WikiSite.class);
        assertThat(adapter.fromJson(adapter.toJson(wiki)), is(wiki));
    }

    @Test public void testReadNull() throws IOException {
        final JsonAdapter<WikiSite> adapter = MoshiUtil.getDefaultMoshi().adapter(WikiSite.class);
        assertThat(adapter.fromJson("null"), nullValue());
    }
}

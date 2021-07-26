package org.wikipedia.json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.wikipedia.json.GsonMarshaller.marshal;
import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

@RunWith(RobolectricTestRunner.class) public class WikiSiteTypeAdapterTest {
    @Test public void testWriteRead() {
        WikiSite wiki = WikiSite.forLanguageCode("test");
        WikiSite wiki2 = unmarshal(WikiSite.class, marshal(wiki));
        assertThat(wiki2.getUri(), is(wiki.getUri()));
        assertThat(wiki2.getLanguageCode(), is(wiki.getLanguageCode()));
    }

    @Test public void testReadNull() {
        assertThat(unmarshal(WikiSite.class, null), nullValue());
    }
}

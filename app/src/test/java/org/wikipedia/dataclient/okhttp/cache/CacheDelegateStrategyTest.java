package org.wikipedia.dataclient.okhttp.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Constants;
import org.wikipedia.test.TestRunner;

import okhttp3.Request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(TestRunner.class) public class CacheDelegateStrategyTest {
    @Test public void testIsCacheableTrue() {
        Request req = new Request.Builder()
                .url(Constants.WIKIPEDIA_URL)
                .addHeader(SaveHeader.FIELD, SaveHeader.VAL_ENABLED)
                .build();
        assertThat(CacheDelegateStrategy.isCacheable(req), is(true));
    }

    @Test public void testIsCacheableFalse() {
        Request req = new Request.Builder().url(Constants.WIKIPEDIA_URL).build();
        assertThat(CacheDelegateStrategy.isCacheable(req), is(false));
    }
}

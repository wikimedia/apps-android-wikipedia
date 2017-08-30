package org.wikipedia.offline;

import android.util.LruCache;

import com.dmitrybrant.zimdroid.DirectoryEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.wikipedia.test.TestFileUtil;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OfflineManagerTest {
    private static final String TEST_ZIM_FILE = "wikipedia_en_ray_charles_2015-06.zim";

    @Mock private LruCache<Integer, DirectoryEntry> mockCache;

    @Test
    public void testOfflineManagerRandom() throws Exception {
        String randomTitle = OfflineManager.instanceNoCache().getRandomTitle();
        assertThat(randomTitle.length(), greaterThan(0));
    }

    @Test
    public void testOfflineManagerSearch() throws Exception {
        List<String> results = OfflineManager.instanceNoCache().searchByPrefix("R", 2);
        assertThat(results.size(), is(2));
        assertThat(results.get(0), is("Raelette"));
    }

    @Test
    public void testOfflineManagerNormalizedTitle() throws Exception {
        String normalizedTitle = OfflineManager.instanceNoCache().getNormalizedTitle("You got the right one baby");
        assertThat(normalizedTitle, is("You Got the Right One, Baby"));
    }

    @Test
    public void testOfflineManagerGetDataForTitle() throws Exception {
        OfflineManager.HtmlResult result = OfflineManager.instanceNoCache().getHtmlForTitle("Ray Charles");
        assertThat(result.html().startsWith("<html>"), is(true));
        assertThat(result.html().endsWith("</html>"), is(true));
    }

    @Before
    public void setup() throws Exception {

        when(mockCache.get(any(Integer.TYPE))).thenReturn(null);

        Compilation compilation = new Compilation(TestFileUtil.getRawFile(TEST_ZIM_FILE),
                mockCache, mockCache);
        OfflineManager.instanceNoCache().setCompilations(Collections.singletonList(compilation));
    }
}

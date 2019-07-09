package org.wikipedia.util;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ImageUriUtilTest {
    private static final int IMAGE_SIZE_1280 = 1280;
    private static final int IMAGE_SIZE_200 = 200;
    private static final String IMAGE_URL_200 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/320px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/1280px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_WITH_NUMERIC_NAME_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/320px-Paeonia_californica_2320679478.jpg";
    private static final String IMAGE_URL_WITH_NUMERIC_NAME_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/1280px-Paeonia_californica_2320679478.jpg";

    @Test
    public void testUrlForSizeURI() {
        Uri uri = Uri.parse(IMAGE_URL_320);
        assertThat(ImageUrlUtil.getUrlForSize(uri, IMAGE_SIZE_1280).toString(), is(IMAGE_URL_320));
    }

    @Test
    public void testUrlForSizeStringWithLargeSize() {
        assertThat(ImageUrlUtil.getUrlForSize(IMAGE_URL_320, IMAGE_SIZE_1280), is(IMAGE_URL_320));
    }

    @Test
    public void testUrlForSizeStringWithSmallSize() {
        assertThat(ImageUrlUtil.getUrlForSize(IMAGE_URL_320, IMAGE_SIZE_200), is(IMAGE_URL_200));
    }

    @Test
    public void testUrlForPreferredSizeWithRegularName() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_320, IMAGE_SIZE_1280), is(IMAGE_URL_1280));
    }

    @Test
    public void testUrlForPreferredSizeWithNumericName() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_WITH_NUMERIC_NAME_320, IMAGE_SIZE_1280), is(IMAGE_URL_WITH_NUMERIC_NAME_1280));
    }
}

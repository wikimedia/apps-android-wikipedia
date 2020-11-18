package org.wikipedia.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImageUriUtilTest {
    private static final int IMAGE_SIZE_1280 = 1280;
    private static final int IMAGE_SIZE_200 = 200;
    private static final String IMAGE_URL_200 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/200px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/320px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/PaeoniaSuffruticosa7.jpg/1280px-PaeoniaSuffruticosa7.jpg";
    private static final String IMAGE_URL_WITH_NUMERIC_NAME_320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/320px-Paeonia_californica_2320679478.jpg";
    private static final String IMAGE_URL_WITH_NUMERIC_NAME_1280 = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Paeonia_californica_2320679478.jpg/1280px-Paeonia_californica_2320679478.jpg";

    @Test
    public void testUrlForSizeStringWithLargeSize() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_320, IMAGE_SIZE_1280), is(IMAGE_URL_1280));
    }

    @Test
    public void testUrlForSizeStringWithSmallSize() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_1280, IMAGE_SIZE_200), is(IMAGE_URL_200));
    }

    @Test
    public void testUrlForSizeStringWithSmallerSize() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_320, IMAGE_SIZE_200), is(IMAGE_URL_200));
    }

    @Test
    public void testUrlForPreferredSizeWithNumericName() {
        assertThat(ImageUrlUtil.getUrlForPreferredSize(IMAGE_URL_WITH_NUMERIC_NAME_320, IMAGE_SIZE_1280), is(IMAGE_URL_WITH_NUMERIC_NAME_1280));
    }
}

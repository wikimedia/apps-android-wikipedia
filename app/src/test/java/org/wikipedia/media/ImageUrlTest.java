package org.wikipedia.media;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.Constants.PREFERRED_THUMB_SIZE;
import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

public class ImageUrlTest {
    private String url1024 = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Istanbul_Airport_Turkish-Airlines_2013-11-18.JPG/1024px-Istanbul_Airport_Turkish-Airlines_2013-11-18.JPG";
    private String url320 = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Istanbul_Airport_Turkish-Airlines_2013-11-18.JPG/320px-Istanbul_Airport_Turkish-Airlines_2013-11-18.JPG";
    private String url244 = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/People%27s_Party_%28Spain%29_Logo.svg/244px-People%27s_Party_%28Spain%29_Logo.svg.png";
    private String urlNoWidth = "https://upload.wikimedia.org/wikipedia/commons/6/6a/Mariano_Rajoy_2015e_%28cropped%29.jpg";

    // Should rewrite URLs for larger images to the desired width, but leave smaller images and
    // image URLs with no width alone.
    @Test public void testImageUrlRewriting() throws Throwable {
        assertThat(getUrlForSize(url1024, PREFERRED_THUMB_SIZE), is(url320));
        assertThat(getUrlForSize(url244, PREFERRED_THUMB_SIZE), is(url244));
        assertThat(getUrlForSize(urlNoWidth, PREFERRED_THUMB_SIZE), is(urlNoWidth));
    }
}

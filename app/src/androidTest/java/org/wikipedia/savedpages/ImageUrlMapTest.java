package org.wikipedia.savedpages;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// todo: move to JUnit tests when https://github.com/robolectric/robolectric/issues/1605 is fixed.
public class ImageUrlMapTest {
    private static final String BASE_DIR = "/data/short/img";

    private ImageUrlMap.Builder builder;

    @Before public void setUp() throws Exception {
        builder = new ImageUrlMap.Builder(BASE_DIR);
    }

    private static final String HTML_INPUT
            = "<div><img alt=\"Alt\" src=\"//foo.org/aaa.png\" width=\"30\"/></div>"
            + "<div><img alt=\"Alt\" src=\"//foo.org/bbb.png\" width=\"30\"/></div>"
            + "<div><img alt=\"Alt\" src=\"//foo.org/aaa.png\" width=\"30\"/></div>"; // repeated the first one

    private static final String IMG_MAP_JSON_OUTPUT
            = "{\"img_map\":["
            + "{\"originalURL\":\"\\/\\/foo.org\\/aaa.png\",\"newURL\":\"file:\\/\\/\\/data\\/short\\/img\\/b1ce85b9f1bd65f79d42ad3358f51f8.png\"},"
            + "{\"originalURL\":\"\\/\\/foo.org\\/bbb.png\",\"newURL\":\"file:\\/\\/\\/data\\/short\\/img\\/a0bf5f6da269a012fefb997167844e3.png\"}"
            + "]}";

    @Test public void testUrlRewrite() throws Exception {
        builder.extractUrlsInSection(HTML_INPUT);
        ImageUrlMap imageUrlMap = builder.build();
        assertThat(imageUrlMap.size(), is(2));
        assertThat(imageUrlMap.toJSON().toString(), is(IMG_MAP_JSON_OUTPUT));
    }

    @Test public void testNonClosedImgTag() throws Exception {
        // abbreviated main page on 2014-06-10; like most main pages right now it has img tags that are not closed
        builder.extractUrlsInSection(
                "<div id=\"mainpage\"><h2>Today's featured article</h2><div id=\"mp-tfa\" style=\"padding:2px 5px\">\n"
                + "<div style=\"float: left; margin: 0.5em 0.9em 0.4em 0em;\"><a href=\"/wiki/File:Fritz_Delius_(1907).jpg\" "
                + "class=\"image\" title=\"Frederick Delius\">\n"
                + "<img alt=\"Frederick Delius\" src=\"//upload.wikimedia.org/wikipedia/en/thumb/7/79/foo.jpg\" "
                + "width=\"100\" height=\"148\" srcset=\"//upload.wikimedia.org/wikipedia/en/thumb/7/79/foo.jpg/150px-foo.jpg 1.5x, "
                + "//upload.wikimedia.org/wikipedia/en/thumb/7/79/Fritz_Delius_%281907%29.jpg/200px-Fritz_Delius_%281907%29.jpg 2x\" \n"
                + "data-file-width=\"1596\" data-file-height=\"2368\"></a></div>\n"
                + "<img alt=\"Maria Sharapova in 2008\" src=\"//upload.wikimedia.org/wikipedia/en/thumb/c/c6/bar.jpg/bar.jpg\" "
                + "width=\"61\" height=\"100\" class=\"thumbborder\" "
                + "srcset=\"//upload.wikimedia.org/wikipedia/en/thumb/c/c6/bar.jpg/91px-bar.jpg 1.5x, "
                + "//upload.wikimedia.org/wikipedia/en/thumb/c/c6/bar.jpg/121px-bar.jpg 2x\" data-file-width=\"405\" \n"
                + "data-file-height=\"667\"></a></div>\n"
                + "</div></div>"
        );
        ImageUrlMap imageUrlMap = builder.build();
        assertThat(imageUrlMap.size(), is(2));
    }
}

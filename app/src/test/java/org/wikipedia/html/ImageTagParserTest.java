package org.wikipedia.html;

import android.support.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ImageTagParserTest {
    private static final PixelDensityDescriptor DESCRIPTOR_1X = new PixelDensityDescriptor(1);
    private static final PixelDensityDescriptor DESCRIPTOR_1_5X = new PixelDensityDescriptor(1.5f);

    @Test public void testParseNoSrcNoSrcSet() {
        assertThat(parse("<img>").src(), nullValue());
    }

    @Test public void testParseNoSrcSet() {
        assertThat(parse("<img src='src'>").src(), is("src"));
    }

    @Test public void testParseNoSrc() {
        ImageElement img = parse("<img srcset='url1 1x'>");
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
    }

    @Test public void testParseSrcAndSrcSet() {
        ImageElement img = parse("<img src='src' srcset='url1 1X'>");
        assertThat(img.src(), is("src"));
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
    }

    @Test public void testParseSrcSetEmpty() {
        ImageElement img = parse("<img srcset=''>");
        assertThat(img.srcs().size(), is(0));
    }

    @Test public void testParseSrcSetBlank() {
        ImageElement img = parse("<img srcset=' '>");
        assertThat(img.srcs().size(), is(0));
    }

    @Test public void testParseSrcSetInvalid() {
        ImageElement img = parse("<img srcset=', url -1x'>");
        assertThat(img.srcs().size(), is(0));
    }

    @Test public void testParseSrcSetMultiple() {
        ImageElement img = parse("<img srcset='url1 1x, url1.5 1.5x'>");
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
        assertThat(img.src(DESCRIPTOR_1_5X), is("url1.5"));
    }

    @Test public void testParsePartiallyInvalid() {
        ImageElement img = parse("<img src='src' srcset='url1 1x, url -, ,, url1.5 1.5x'>");
        assertThat(img.src(), is("src"));
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
        assertThat(img.src(DESCRIPTOR_1_5X), is("url1.5"));
    }

    @Test public void testParseNoDescriptor() {
        ImageElement img = parse("<img srcset='url1'>");
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
    }

    @Test public void testParseMixedDescriptors() {
        ImageElement img = parse("<img srcset='url1, url1.5 1.5x'>");
        assertThat(img.src(DESCRIPTOR_1X), is("url1"));
        assertThat(img.src(DESCRIPTOR_1_5X), is("url1.5"));
    }

    @NonNull private ImageElement parse(@NonNull String html) {
        ImageTagParser subject = new ImageTagParser();
        PixelDensityDescriptorParser descriptorParser = new PixelDensityDescriptorParser();
        Element el = Jsoup.parseBodyFragment(html).getElementsByTag("img").first();
        return subject.parse(descriptorParser, el);
    }
}

package org.wikipedia.html;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ImageElementTest {
    @Test public void testSrcDefaultEmptySrcSet() {
        String src = "src";
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet = Collections.emptyMap();
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.src(), is(src));
    }

    @Test public void testSrcDefaultNonemptySrcSet() {
        String src = "src";
        PixelDensityDescriptor descriptor = new PixelDensityDescriptor(1);
        String nondefaultUrl = "url";
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet
                = Collections.singletonMap(descriptor, nondefaultUrl);
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.src(), is(src));
    }

    @Test public void testSrcNondefaultNonemptySrcSet() {
        String src = "src";
        PixelDensityDescriptor descriptor = new PixelDensityDescriptor(1);
        String nondefaultUrl = "url";
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet
                = Collections.singletonMap(descriptor, nondefaultUrl);
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.src(descriptor), is(nondefaultUrl));
    }

    @Test public void testSrcNondefaultNoDefault() {
        final String src = null;
        PixelDensityDescriptor descriptor = new PixelDensityDescriptor(1);
        String nondefaultUrl = "url";
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet
                = Collections.singletonMap(descriptor, nondefaultUrl);
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.src(), nullValue());
        assertThat(subject.src(descriptor), is(nondefaultUrl));
    }

    @Test public void testSrcsEmpty() {
        final String src = null;
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet
                = Collections.emptyMap();
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.srcs().size(), is(0));
    }

    @Test public void testSrcsNonempty() {
        String src = "src";
        PixelDensityDescriptor descriptor = new PixelDensityDescriptor(1);
        String nondefaultUrl = "url";
        @SuppressWarnings("unchecked") Map<PixelDensityDescriptor, String> srcSet
                = Collections.singletonMap(descriptor, nondefaultUrl);
        ImageElement subject = new ImageElement(src, srcSet);
        assertThat(subject.srcs().size(), is(2));
    }
}

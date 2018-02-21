package org.wikipedia.html;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PixelDensityDescriptorTest {
    @Test public void testDensity() {
        PixelDensityDescriptor subject = new PixelDensityDescriptor(1);
        assertThat(subject.density(), is(1f));
    }
}

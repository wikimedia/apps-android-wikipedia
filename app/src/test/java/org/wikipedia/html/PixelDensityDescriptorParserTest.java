package org.wikipedia.html;

import android.support.annotation.NonNull;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PixelDensityDescriptorParserTest {
    @NonNull private final PixelDensityDescriptorParser subject = new PixelDensityDescriptorParser();

    @Test(expected = ParseException.class) public void testParseEmpty() {
        subject.parse("");
    }

    @Test(expected = ParseException.class) public void testParseBlank() {
        subject.parse(" ");
    }

    @Test(expected = ParseException.class) public void testParseWidthDescriptor() {
        subject.parse("200w");
    }

    @Test(expected = ParseException.class) public void testParseUnspecifiedDescriptor() {
        subject.parse("1");
    }

    @Test(expected = ParseException.class) public void testParseNegative() {
        subject.parse("-1x");
    }

    @Test(expected = ParseException.class) public void testParseZero() {
        subject.parse("0x");
    }

    @Test public void testParsePositive() {
        assertThat(subject.parse("1x").density(), is(1f));
    }

    @SuppressWarnings("checkstyle:magicnumber") @Test public void testParseFloatingPoint() {
        assertThat(subject.parse("1.5x").density(), is(1.5f));
    }

    @Test public void testParseUppercase() {
        assertThat(subject.parse("1X").density(), is(1f));
    }
}

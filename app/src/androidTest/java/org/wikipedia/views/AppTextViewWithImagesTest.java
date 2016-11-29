package org.wikipedia.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.test.InstrumentationRegistry;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.text.style.ImageSpan;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.R;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppTextViewWithImagesTest {
    private AppTextViewWithImages subject;
    private Context ctx = InstrumentationRegistry.getTargetContext();

    @Before public void setUp() {
        subject = new AppTextViewWithImages(ctx);
    }

    @Test public void testSetTextWithDrawablesWithOneExpectedImageSpan() {
        subject.setTextWithDrawables("Every good ^1 does fine.", R.drawable.edit);
        ImageSpan[] spans = subject.getSpans(ImageSpan.class);
        assertThat(spans.length, is(1));
    }

    @Test public void testSetTextWithDrawablesWithTwoExpectedImageSpans() {
        subject.setTextWithDrawables("Every good ^1 does ^2.", R.drawable.edit, R.drawable.cc_logo);
        ImageSpan[] spans = subject.getSpans(ImageSpan.class);
        assertThat(spans.length, is(2));
    }

    @Test public void testSetTextWithDrawablesWithNoResourcesSpecified() {
        subject.setTextWithDrawables("Every good ^1 does ^2.");
        CharSequence text = subject.getText();
        assertThat(text.toString(), is("Every good ^1 does ^2."));
    }

    @Test public void testSetWithDrawablesWithEmptyString() {
        subject.setTextWithDrawables("", R.drawable.edit, R.drawable.cc_logo);
        CharSequence text = subject.getText();
        assertThat(text.toString(), is(""));
    }

    @Test public void testMakeImageSpan() {
        Spanned spanned = subject.makeImageSpan(R.drawable.edit, 10, getColor(android.R.color.black));
        assertThat(spanned, notNullValue());
    }

    @Test public void testGetFormattedDrawable() {
        Drawable edit = subject.getFormattedDrawable(R.drawable.edit, 10, getColor(android.R.color.black));
        assertThat(edit, notNullValue());
    }

    @ColorInt private int getColor(@ColorRes int id) {
        return ContextCompat.getColor(ctx, id);
    }
}

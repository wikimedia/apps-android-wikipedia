package org.wikipedia.views;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.TertiaryTestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppTextViewWithImagesTest extends ViewTest {
    private AppTextViewWithImages subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XS, WIDTH_DP_S, WIDTH_DP_M, WIDTH_DP_XL}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull TertiaryTestStr text) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
        subject.setTextWithDrawables("^1 " + str(text)  + " ^2", R.drawable.ic_mode_edit_white_24dp, R.drawable.cc_logo);
        snap(subject, text + "_text");
    }

    @Theory public void testLayoutDirection(@TestedOn(ints = {WIDTH_DP_XS, WIDTH_DP_M}) int widthDp,
                                            @NonNull LayoutDirection direction) {
        setUp(widthDp, direction, FontScale.DEFAULT, Theme.LIGHT);
        subject.setTextWithDrawables("Every good ^1 does ^2.", R.drawable.ic_mode_edit_white_24dp, R.drawable.cc_logo);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
        subject.setTextWithDrawables("Every good ^1 does ^2.", R.drawable.ic_mode_edit_white_24dp, R.drawable.cc_logo);
        snap(subject);
    }

    @Test public void testSetTextWithDrawablesWithOneExpectedImageSpan() {
        setUpTypical();
        subject.setTextWithDrawables("Every good ^1 does fine.", R.drawable.ic_mode_edit_white_24dp);
        ImageSpan[] spans = subject.getSpans(ImageSpan.class);
        assertThat(spans.length, is(1));
    }

    @Test public void testSetTextWithDrawablesWithTwoExpectedImageSpans() {
        setUpTypical();
        subject.setTextWithDrawables("Every good ^1 does ^2.", R.drawable.ic_mode_edit_white_24dp, R.drawable.cc_logo);
        ImageSpan[] spans = subject.getSpans(ImageSpan.class);
        assertThat(spans.length, is(2));
    }

    @Test public void testSetTextWithDrawablesWithNoResourcesSpecified() {
        setUpTypical();
        subject.setTextWithDrawables("Every good ^1 does ^2.");
        CharSequence text = subject.getText();
        assertThat(text.toString(), is("Every good ^1 does ^2."));
    }

    @Test public void testSetWithDrawablesWithEmptyString() {
        setUpTypical();
        subject.setTextWithDrawables("", R.drawable.ic_mode_edit_white_24dp, R.drawable.cc_logo);
        CharSequence text = subject.getText();
        assertThat(text.toString(), is(""));
    }

    @Test public void testMakeImageSpan() {
        setUpTypical();
        Spanned spanned = subject.makeImageSpan(R.drawable.ic_mode_edit_white_24dp, 10, getColor(android.R.color.black));
        assertThat(spanned, notNullValue());
    }

    @Test public void testGetFormattedDrawable() {
        setUpTypical();
        Drawable edit = subject.getFormattedDrawable(R.drawable.ic_mode_edit_white_24dp, 10, getColor(android.R.color.black));
        assertThat(edit, notNullValue());
    }

    @Override protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                                   @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        subject = new AppTextViewWithImages(ctx());
        subject.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private void setUpTypical() {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
    }

    @ColorInt private int getColor(@ColorRes int id) {
        return ContextCompat.getColor(ctx(), id);
    }
}

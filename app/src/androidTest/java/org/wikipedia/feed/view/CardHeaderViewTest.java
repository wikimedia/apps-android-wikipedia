package org.wikipedia.feed.view;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.filters.SmallTest;
import android.support.v4.view.TintableBackgroundView;
import android.widget.ImageView;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static butterknife.ButterKnife.findById;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.wikipedia.test.TestUtil.runOnMainSync;

@SmallTest public class CardHeaderViewTest extends ViewTest {
    private CardHeaderView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  float fontScale,
                                  @TestedOn(ints = {0, R.drawable.wmf_logo}) int image,
                                  @TestedOn(ints = {0, R.string.reading_list_name_sample,
                                          R.string.gallery_save_image_write_permission_rationale}) int title,
                                  @TestedOn(ints = {0, R.string.reading_list_untitled,
                                          R.string.reading_lists_empty_message}) int subtitle) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title, subtitle,
                R.color.foundation_blue);
        snap(subject, image == 0 ? "no_image" : "image", len(title) + "title",
                len(subtitle) + "subtitle");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled,
                R.color.foundation_blue);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme,
                                  @TestedOn(ints = {R.color.foundation_blue, R.color.foundation_green}) int circleColor) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, theme, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled, circleColor);
        snap(subject, circleColor == R.color.foundation_blue ? "blue" : "green");
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, theme, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled,
                R.color.foundation_blue);
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.requestFocus();
            }
        });
        snap(subject);
    }

    // todo: how can we test popupmenu which requires an activity?
//    @Theory public void testMenu(@NonNull Theme theme) {
//        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, theme, R.drawable.wmf_logo,
//                R.string.reading_list_name_sample, R.string.reading_list_untitled);
//        clickMenu();
//    }

    @Test public void testSetCard() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, 0, 0, 0, R.color.foundation_blue);
        Card card = mock(Card.class);
        subject.setCard(card);
        assertThat(subject.getCard(), is(card));
    }

    // todo: how can we test popupmenu which requires an activity?
//    @Theory public void testSetCallback(@TestedOn(ints = {0, 1}) int nonnullListener,
//                                        @TestedOn(ints = {0, 1}) int nonnullCard) {
//        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo,
//                R.string.reading_list_name_sample, R.string.reading_list_untitled);
//
//        Card card = nonnullCard == 0 ? null : mock(Card.class);
//        if (nonnullCard != 0) {
//            subject.setCard(card);
//        }
//        Callback callback = nonnullListener == 0 ? null : mock(Callback.class);
//        subject.setCallback(callback);
//        clickMenu();
//        if (callback != null) {
//            verify(callback).onRequestDismissCard(eq(card));
//        }
//    }

    @Test public void testSetImage() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled,
                R.color.foundation_blue);
        ImageView imageView = findById(subject, R.id.view_card_header_image);
        imageView.setImageDrawable(null);
        subject.setImage(R.drawable.checkerboard);
        assertThat(imageView.getDrawable(), notNullValue());
    }

    @Test public void testSetImageCircleColor() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled,
                R.color.foundation_blue);
        TintableBackgroundView imageView = findById(subject, R.id.view_card_header_image);
        imageView.setSupportBackgroundTintList(null);
        subject.setImageCircleColor(R.color.foundation_blue);
        assertThat(imageView.getSupportBackgroundTintList(), notNullValue());
    }

    @Theory public void testSetTitleStr(@TestedOn(ints = {0,
            R.string.reading_list_name_sample}) int text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo, text,
                R.string.reading_list_untitled, R.color.foundation_blue);
        assertText(subject, R.id.view_card_header_title, text);
    }

    @Test public void testSetTitleId() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_name_sample, R.string.reading_list_untitled,
                R.color.foundation_blue);
        assertText(subject, R.id.view_card_header_title, R.string.reading_list_name_sample);
    }

    @Theory public void testSetSubtitle(@TestedOn(ints = {0,
            R.string.reading_list_name_sample}) int text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_untitled, text, R.color.foundation_blue);
        assertText(subject, R.id.view_card_header_subtitle, text);
    }

//    private void clickMenu() {
//        runOnMainSync(new Runnable() {
//            @Override public void run() {
//                subject.onMenuClick(findById(subject, R.id.view_list_card_header_menu));
//            }
//        });
//    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection, float fontScale,
                       @NonNull Theme theme, @DrawableRes int image, @StringRes int title,
                       @StringRes int subtitle, @ColorRes int circleColor) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new CardHeaderView(ctx())
                .setImage(image)
                .setImageCircleColor(circleColor)
                .setTitle(str(title))
                .setSubtitle(str(subtitle));
    }
}
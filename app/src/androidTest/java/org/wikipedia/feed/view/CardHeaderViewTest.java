package org.wikipedia.feed.view;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.widget.ImageView;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestImg;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestImg;
import org.wikipedia.test.view.SecondaryTestStr;
import org.wikipedia.test.view.TestImg;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static butterknife.ButterKnife.findById;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class CardHeaderViewTest extends ViewTest {
    @ColorRes private static final int BLUE = R.color.accent50;
    private CardHeaderView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestImg image,
                                  @NonNull PrimaryTestStr title, @NonNull SecondaryTestStr subtitle) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title, subtitle,
                BLUE);
        snap(subject, image + "_image", title + "_title", subtitle + "_subtitle");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme,
                                  @TestedOn(ints = {BLUE, R.color.green50}) int circleColor) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, circleColor);
        snap(subject, circleColor == BLUE ? "blue" : "green");
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
        requestFocus(subject);
        snap(subject);
    }

    // todo: how can we test popupmenu which requires an activity?
//    @Theory public void testMenu(@NonNull Theme theme) {
//        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NONNULL,
//                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
//        clickMenu();
//    }

    @Test public void testSetCard() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NULL, PrimaryTestStr.NULL, SecondaryTestStr.NULL, BLUE);
        Card card = mock(Card.class);
        subject.setCard(card);
        assertThat(subject.getCard(), is(card));
    }

    // todo: how can we test popupmenu which requires an activity?
//    @Theory public void testSetCallback(@TestedOnBool boolean nullCallback,
//                                        @TestedOnBool boolean nullCard) {
//        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
//                PrimaryTestImg.NONNULL, PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
//
//        Card card = nullCard ? null : mock(Card.class);
//        if (card != null) {
//            subject.setCard(card);
//        }
//        Callback callback = nullCallback ? null : mock(Callback.class);
//        subject.setCallback(callback);
//        clickMenu();
//        if (callback != null) {
//            verify(callback).onRequestDismissCard(eq(card));
//        }
//    }

    @Test public void testSetImage() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT,
                R.color.accent50);
        ImageView imageView = findById(subject, R.id.view_card_header_image);
        imageView.setImageDrawable(null);
        subject.setImage(SecondaryTestImg.CHECKERBOARD.id());
        assertThat(imageView.getDrawable(), notNullValue());
    }

    @Test public void testSetImageCircleColor() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
        AppCompatImageView imageView = findById(subject, R.id.view_card_header_image);
        ViewCompat.setBackgroundTintList(imageView, null);
        subject.setImageCircleColor(BLUE);
        assertThat(ViewCompat.getBackgroundTintList(imageView), notNullValue());
    }

    @Theory public void testSetTitleStr(@NonNull PrimaryTestStr text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL, text,
                SecondaryTestStr.SHORT, BLUE);
        assertText(subject, R.id.view_card_header_title, text);
    }

    @Test public void testSetTitleId() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT, BLUE);
        assertText(subject, R.id.view_card_header_title, PrimaryTestStr.SHORT);
    }

    @Theory public void testSetSubtitle(@NonNull PrimaryTestStr text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                SecondaryTestStr.SHORT, text, BLUE);
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
    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestImg image,
                       @NonNull TestStr title, @NonNull TestStr subtitle, @ColorRes int circleColor) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new CardHeaderView(ctx())
                .setImage(image.id())
                .setImageCircleColor(circleColor)
                .setTitle(str(title))
                .setSubtitle(str(subtitle));
    }
}

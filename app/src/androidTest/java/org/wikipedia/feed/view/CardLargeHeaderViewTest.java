package org.wikipedia.feed.view;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.filters.SmallTest;
import android.view.View;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static android.view.View.OnClickListener;
import static butterknife.ButterKnife.findById;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SmallTest public class CardLargeHeaderViewTest extends ViewTest {
    private CardLargeHeaderView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  float fontScale,
                                  @TestedOn(ints = {0, R.drawable.checkerboard}) int image,
                                  @TestedOn(ints = {0, R.string.reading_list_name_sample,
                                          R.string.gallery_save_image_write_permission_rationale}) int title) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title);
        snap(subject, image == 0 ? "no_image" : "image", len(title) + "title");
    }

    @Theory public void testLayoutDirection(LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, 1, Theme.LIGHT, R.drawable.wmf_logo,
                R.string.reading_list_name_sample);
        snap(subject);
    }

    @Theory public void testTheme(Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, theme, 0, R.string.reading_list_name_sample);
        snap(subject);
    }

    @Theory public void testFocus(Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, theme, 0, R.string.reading_list_name_sample);
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.requestFocus();
            }
        });
        snap(subject);
    }

    @Theory public void testSetImage(@TestedOn(ints = {0, R.drawable.checkerboard}) int image) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, image, 0);
        View imageView = findById(subject, R.id.view_card_header_large_image);
        assertThat(imageView.getVisibility(), is(image == 0 ? View.GONE : View.VISIBLE));
    }

    @Theory public void testSetTitle(@TestedOn(ints = {0,
            R.string.reading_list_name_sample}) int text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, 0, text);
        assertText(subject, R.id.view_card_header_large_title, text);
    }

    @Theory public void testOnClickListener(@TestedOn(ints = {0, 1}) int nonnull) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, 1, Theme.LIGHT, R.drawable.checkerboard,
                R.string.reading_list_name_sample);

        OnClickListener listener = nonnull == 0 ? null : mock(View.OnClickListener.class);
        subject.onClickListener(listener);
        subject.performClick();
        if (listener != null) {
            verify(listener);
        }
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection, float fontScale,
                       @NonNull Theme theme, @DrawableRes int image, @StringRes int title) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new CardLargeHeaderView(ctx())
                .setImage(frescoUri(image))
                .setTitle(str(title));
    }
}
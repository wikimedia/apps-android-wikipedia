package org.wikipedia.page.leadimages;

import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.facebook.testing.screenshot.ViewHelpers;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.Constants;
import org.wikipedia.page.leadimages.PageHeaderView.Callback;
import org.wikipedia.test.theories.TestedOnBool;
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
import org.wikipedia.views.FaceAndColorDetectImageView.OnImageLoadListener;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.wikipedia.test.TestUtil.runOnMainSync;

public class PageHeaderViewTest extends ViewTest {
    private PageHeaderView subject;

    @Theory public void testLayout(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                   // @TestedOn(ints = {HEIGHT_DP_L, WIDTH_DP_S}) int heightDp,
                                   @NonNull FontScale fontScale, @NonNull PrimaryTestImg image,
                                   @NonNull PrimaryTestStr title, @NonNull SecondaryTestStr subtitle,
                                   @TestedOnBool boolean pronunciation) {
        // todo: pass height when layout is correct
        setUp(widthDp, 0, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title,
                subtitle, pronunciation);
        snap(subject, image + "_image", title + "_title", subtitle + "_subtitle",
                pronunciation ? "pronunciation" : "no_pronunciation");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, HEIGHT_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NONNULL, PrimaryTestStr.SHORT, PrimaryTestStr.SHORT, true);
        subject.setLocale(direction.isRtl() ? "he" : LOCALES[0].getLanguage());
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, HEIGHT_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme,
                PrimaryTestImg.NONNULL, PrimaryTestStr.SHORT, PrimaryTestStr.SHORT, true);
        snap(subject);
    }

    @Test public void testHide() {
        setUpTypical();
        subject.hide();
        assertThat(subject.getVisibility(), is(View.GONE));
    }

    @Test public void testShowTextTitleContent() {
        setUpTypical();
        String text = "text";
        setTitle(text);
        showText();
        assertThat(subject.titleText.getText().toString(), startsWith(text));
    }

    @Theory public void testShowTextSubtitleContent(@TestedOnBool boolean nul) {
        setUpTypical();
        String text = nul ? null : "text";
        setSubtitle(text);
        showText();
        assertThat(subject.subtitleText.getText().length(), greaterThan(0));
        if (text != null) {
            // null case uses placeholder text
            assertThat(subject.subtitleText.getText().toString(), is(text));
        }
    }

    @Theory public void testShowTextVisible(@TestedOnBool boolean afterHide) {
        setUpTypical();
        if (afterHide) {
            subject.hide();
        }
        showText();
        assertThat(subject.getVisibility(), is(View.VISIBLE));
    }

    @Theory public void testShowTextImageVisible(@TestedOnBool boolean afterHide) {
        setUpTypical();
        if (afterHide) {
            subject.hide();
        }
        showTextImage();
        assertThat(subject.getVisibility(), is(View.VISIBLE));
    }

    @Theory public void testGetImage(@TestedOnBool boolean nul) {
        setUpTypical();
        String url = nul ? null : frescoUri(SecondaryTestImg.CHECKERBOARD.id()).toString();
        subject.loadImage(url);
        assertThat(subject.getImage(), notNullValue());
    }

    @Theory public void testSetOnImageLoadListener(@TestedOnBool boolean nul,
                                                   @TestedOnBool final boolean fail) {
        setUpTypical();

        OnImageLoadListener listener = nul ? null : mock(OnImageLoadListener.class);
        subject.setOnImageLoadListener(listener);
        String url = fail ? "http://" : frescoUri(SecondaryTestImg.CHECKERBOARD.id()).toString();
        subject.loadImage(url);
        // todo: wait for image to load and verify
        // if (listener != null) {
            // if (fail) {
            //     verify(listener, never()).onImageLoaded(anyInt(), any(PointF.class), anyInt());
            //     verify(listener).onImageFailed();
            // } else {
            //     verify(listener).onImageLoaded(anyInt(), any(PointF.class), anyInt());
            //     verify(listener, never()).onImageFailed();
            // }
        // }
    }

    @Theory public void testSetCallback(@TestedOnBool boolean nul,
                                        @TestedOnBool boolean descriptionClicked) {
        setUpTypical();

        Callback callback = nul ? null : mock(Callback.class);
        subject.setCallback(callback);

        if (descriptionClicked) {
            subject.descriptionClickSpan.onClick(subject);
        } else {
            // todo: how to test activity?
            // subject.editPencil.performClick();
        }

        if (callback != null) {
            if (descriptionClicked) {
                verify(callback).onDescriptionClicked();
                verify(callback, never()).onEditDescription();
                verify(callback, never()).onEditLeadSection();
            } else {
                verify(callback, never()).onDescriptionClicked();
                verify(callback, never()).onEditDescription();
                // todo: how to test activity?
                // verify(callback).onEditLeadSection();
            }
        }
    }

    @Theory public void testLoadImage(@TestedOnBool boolean nul) {
        setUpTypical();
        String url = nul ? null : frescoUri(SecondaryTestImg.CHECKERBOARD.id()).toString();
        subject.loadImage(url);
        assertThat(subject.getMinimumHeight(), nul ? is(0) : greaterThan(0));
    }

    @Theory public void testSetAnimationPaused(@TestedOnBool boolean nul,
                                               @TestedOnBool boolean paused) {
        setUpTypical();
        String url = nul ? null : frescoUri(SecondaryTestImg.CHECKERBOARD.id()).toString();
        subject.loadImage(url);
        subject.setAnimationPaused(paused);
        if (!nul) {
            // todo: wait for image to load and verify
            // assertThat(subject.image.getImage().getController().getAnimatable().isRunning(), is(paused));
        }
    }

    @Test public void testCopyBitmap() {
        setUpTypical();

        // width and height must be > 0 for Bitmap.createBitmap()
        final int positive = 100;
        ViewHelpers.setupView(subject).setExactWidthDp(positive).setExactHeightDp(positive).layout();

        runOnMainSync(new Runnable() {
            @Override public void run() {
                assertThat(subject.copyBitmap(), notNullValue());
            }
        });
    }

    @Test public void testSetImageFocus() {
        setUpTypical();
        subject.setImageFocus(new PointF());
        // todo: verify
    }

    @Theory public void testSetTitle(@TestedOnBool boolean nul) {
        setUpTypical();
        CharSequence text = nul ? null : "text";
        setTitle(text);
        assertThat(subject.title, is(defaultIfEmpty(text, "")));
    }

    @Theory public void testSetSubtitle(@TestedOnBool boolean nul) {
        setUpTypical();
        CharSequence text = nul ? null : "text";
        setSubtitle(text);
        assertThat(subject.subtitle, is(defaultIfEmpty(text, "")));
    }

    @Theory public void testHasSubtitle(@TestedOnBool boolean nul) {
        setUpTypical();
        setSubtitle(nul ? null : "text");
        assertThat(subject.hasSubtitle(), is(!nul));
    }

    @Test public void testSetLocale() {
        setUpTypical();
        subject.setLocale(LOCALES[0].getLanguage());
        // todo: how to verify?
    }

    @Theory public void testSetPronunciation(@TestedOnBool boolean nul) {
        setUpTypical();
        String url = nul ? null : Constants.WIKIPEDIA_URL;
        setPronunciation(url);
        assertThat(subject.pronunciationUrl, is(url));
    }

    @Theory public void testHasPronunciation(@TestedOnBool boolean nul) {
        setUpTypical();
        setPronunciation(nul ? null : Constants.WIKIPEDIA_URL);
        assertThat(subject.hasPronunciation(), is(!nul));
    }

    @Theory public void testOnScrollChanged(@TestedOn(ints = {-1, 0, 1}) int y) {
        setUpTypical();

        // width and height must be > 0 for Bitmap.createBitmap()
        final int height = 100;
        ViewHelpers.setupView(subject).setExactHeightDp(height).layout();

        subject.onScrollChanged(0, y, false);
        assertThat((int) subject.getTranslationY(), is(-Math.min(height, y)));
        assertThat((int) subject.image.getImage().getTranslationY(), is(y / 2));
    }

    private void showText() {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.showText();
            }
        });
    }

    private void showTextImage() {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.showTextImage();
            }
        });
    }

    private void setTitle(@Nullable final CharSequence text) {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.setTitle(text);
            }
        });
    }

    private void setSubtitle(@Nullable final CharSequence text) {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.setSubtitle(text);
            }
        });
    }

    private void setPronunciation(@Nullable final String url) {
        runOnMainSync(new Runnable() {
            @Override public void run() {
                subject.setPronunciation(url);
            }
        });
    }

    private void setUpTypical() {
        setUp(WIDTH_DP_M, HEIGHT_DP_M, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NONNULL, PrimaryTestStr.LONG, SecondaryTestStr.LONG, true);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void setUp(int widthDp, int heightDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestImg image,
                       @NonNull TestStr title, @NonNull TestStr subtitle,
                       final boolean pronunciation) {
        // todo: pass height
        //setUp(widthDp, heightDp, layoutDirection, fontScale, theme);
        setUp(widthDp, layoutDirection, fontScale, theme);

        Uri imageUri = frescoUri(image.id());
        subject = new PageHeaderView(ctx());
        subject.loadImage(imageUri == null ? null : imageUri.toString());
        setTitle(str(title));
        setSubtitle(str(subtitle));
        subject.setLocale(LOCALES[0].getLanguage());
        setPronunciation(pronunciation ? Constants.WIKIPEDIA_URL : null);
    }
}

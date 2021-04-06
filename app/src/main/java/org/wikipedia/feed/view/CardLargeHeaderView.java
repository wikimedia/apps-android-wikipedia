package org.wikipedia.feed.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.palette.graphics.Palette;

import com.google.android.material.card.MaterialCardView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.TransitionUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("checkstyle:magicnumber")
public class CardLargeHeaderView extends ConstraintLayout {
    @BindView(R.id.view_card_header_large_border_container) View borderContainer;
    @BindView(R.id.view_card_header_large_border_base) MaterialCardView borderBaseView;
    @BindView(R.id.view_card_header_large_container) ConstraintLayout container;
    @BindView(R.id.view_card_header_large_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_card_header_large_title) TextView titleView;
    @BindView(R.id.view_card_header_large_subtitle) TextView subtitleView;

    public CardLargeHeaderView(Context context) {
        super(context);
        init();
    }

    public CardLargeHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardLargeHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_card_header_large, this);
        ButterKnife.bind(this);
        resetBackgroundColor();
    }

    @NonNull
    public CardLargeHeaderView setLanguageCode(@NonNull String langCode) {
        L10nUtil.setConditionalLayoutDirection(this, langCode);
        return this;
    }

    @NonNull
    public CardLargeHeaderView setImage(@Nullable Uri uri) {
        imageView.setVisibility(uri == null ? GONE : VISIBLE);
        imageView.loadImage(uri, true, new ImageLoadListener());
        return this;
    }

    @NonNull
    public CardLargeHeaderView setTitle(@Nullable String title) {
        titleView.setText(StringUtil.fromHtml(title));
        return this;
    }

    @NonNull
    public CardLargeHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(subtitle);
        return this;
    }

    private void resetBackgroundColor() {
        setGradientDrawableBackground(ContextCompat.getColor(getContext(), R.color.base100),
                ContextCompat.getColor(getContext(), R.color.base20));
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(@NonNull Palette palette, int bmpWidth, int bmpHeight) {
            int color1 = palette.getLightVibrantColor(ContextCompat.getColor(getContext(), R.color.base70));
            int color2 = palette.getLightMutedColor(ContextCompat.getColor(getContext(), R.color.base30));
            if (WikipediaApp.getInstance().getCurrentTheme().isDark()) {
                color1 = darkenColor(color1);
                color2 = darkenColor(color2);
            } else {
                color1 = lightenColor(color1);
                color2 = lightenColor(color2);
            }
            setGradientDrawableBackground(color1, color2);
        }

        @Override
        public void onImageFailed() {
            resetBackgroundColor();
        }
    }

    private static int lightenColor(@ColorInt int color) {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.3f);
    }

    private static int darkenColor(@ColorInt int color) {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.3f);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setGradientDrawableBackground(@ColorInt int leftColor, @ColorInt int rightColor) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {leftColor, rightColor});

        // card background
        gradientDrawable.setAlpha(70);
        gradientDrawable.setCornerRadius(borderBaseView.getRadius());
        container.setBackground(gradientDrawable);

        // card border's background, which depends on the margin that is applied to the borderBaseView
        gradientDrawable.setAlpha(90);
        gradientDrawable.setCornerRadius(getResources().getDimension(R.dimen.wiki_card_radius));
        borderContainer.setBackground(gradientDrawable);
    }

    public Pair<View, String>[] getSharedElements() {
        return TransitionUtil.getSharedElements(getContext(), imageView);
    }
}

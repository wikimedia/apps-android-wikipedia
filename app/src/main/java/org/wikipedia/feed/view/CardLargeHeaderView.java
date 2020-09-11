package org.wikipedia.feed.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends ConstraintLayout {
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
        resetBackgroundColor();
        inflate(getContext(), R.layout.view_card_header_large, this);
        ButterKnife.bind(this);
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
        public void onImageLoaded(@NonNull Palette palette) {
            setGradientDrawableBackground(palette.getLightMutedColor(ContextCompat.getColor(getContext(), R.color.base100)),
                    palette.getMutedColor(ContextCompat.getColor(getContext(), R.color.base20)));
        }

        @Override
        public void onImageFailed() {
            resetBackgroundColor();
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setGradientDrawableBackground(@ColorInt int leftColor, @ColorInt int rightColor) {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { leftColor, rightColor });
        gradientDrawable.setAlpha(70);
        setBackground(gradientDrawable);
    }
}

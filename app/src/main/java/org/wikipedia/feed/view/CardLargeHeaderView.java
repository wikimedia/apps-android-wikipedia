package org.wikipedia.feed.view;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends FrameLayout {
    @BindView(R.id.view_card_header_large_background) View backgroundView;
    @BindView(R.id.view_card_header_large_text_container) View textContainerView;
    @BindView(R.id.view_card_header_large_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_card_header_large_page_title) TextView pageTitleView;
    @BindView(R.id.view_card_header_large_subtitle) GoneIfEmptyTextView subtitleView;

    public CardLargeHeaderView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_card_header_large, this);
        ButterKnife.bind(this);
        imageView.setOnImageLoadListener(new ImageLoadListener());
        resetBackgroundColor();
    }

    @NonNull public CardLargeHeaderView setImage(@Nullable Uri uri) {
        imageView.setVisibility(uri == null ? GONE : VISIBLE);
        if (uri != null) {
            imageView.loadImage(uri);
        }
        return this;
    }

    @NonNull public CardLargeHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(subtitle == null ? null : subtitle.toString());
        return this;
    }

    @NonNull public CardLargeHeaderView setPageTitle(@Nullable CharSequence title) {
        pageTitleView.setText(title);
        return this;
    }

    @NonNull public CardLargeHeaderView onClickListener(@Nullable OnClickListener listener) {
        textContainerView.setOnClickListener(listener);
        return this;
    }

    private void resetBackgroundColor() {
        backgroundView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.gray_background));
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(final int bmpHeight, @Nullable final PointF faceLocation, @ColorInt final int mainColor) {
            backgroundView.post(new Runnable() {
                @Override
                public void run() {
                    animateBackgroundColor(backgroundView, mainColor);
                }
            });
        }

        @Override
        public void onImageFailed() {
            resetBackgroundColor();
        }

        public void animateBackgroundColor(@NonNull View view, @ColorInt int targetColor) {
            final int animDuration = 500;
            final ObjectAnimator animator = ObjectAnimator.ofObject(view.getBackground(), "color",
                    new ArgbEvaluator(), targetColor);
            animator.setDuration(animDuration);
            animator.start();
        }
    }
}
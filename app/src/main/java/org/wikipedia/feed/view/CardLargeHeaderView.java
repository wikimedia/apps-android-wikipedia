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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends RelativeLayout {
    @BindView(R.id.view_card_header_large_background) View backgroundView;
    @BindView(R.id.view_card_header_large_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_card_header_large_title) TextView titleView;

    public CardLargeHeaderView(Context context) {
        super(context);

        resetBackgroundColor();
        inflate(getContext(), R.layout.view_card_header_large, this);
        ButterKnife.bind(this);
        imageView.setOnImageLoadListener(new ImageLoadListener());
    }

    @NonNull public CardLargeHeaderView setImage(@Nullable Uri uri) {
        imageView.setVisibility(uri == null ? GONE : VISIBLE);
        imageView.loadImage(uri);
        return this;
    }

    @NonNull public CardLargeHeaderView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public CardLargeHeaderView onClickListener(@Nullable OnClickListener listener) {
        backgroundView.setOnClickListener(listener);
        return this;
    }

    private void resetBackgroundColor() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.gray_background));
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(final int bmpHeight, @Nullable final PointF faceLocation, @ColorInt final int mainColor) {
            post(new Runnable() {
                @Override
                public void run() {
                    animateBackgroundColor(CardLargeHeaderView.this, mainColor);
                    if (faceLocation != null) {
                        imageView.getHierarchy().setActualImageFocusPoint(faceLocation);
                    }
                }
            });
        }

        @Override
        public void onImageFailed() {
            resetBackgroundColor();
        }

        private void animateBackgroundColor(@NonNull View view, @ColorInt int targetColor) {
            final int animDuration = 500;
            final ObjectAnimator animator = ObjectAnimator.ofObject(view.getBackground(), "color",
                    new ArgbEvaluator(), targetColor);
            animator.setDuration(animDuration);
            animator.start();
        }
    }
}
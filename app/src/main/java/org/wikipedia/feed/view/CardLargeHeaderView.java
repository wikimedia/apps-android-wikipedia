package org.wikipedia.feed.view;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends ConstraintLayout {
    @BindView(R.id.view_card_header_large_background) View backgroundView;
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
        imageView.setOnImageLoadListener(new ImageLoadListener());
    }

    // todo: should this use ViewUtil.loadImageUrlInto() instead? should loadImageUrlInto() set
    //       view visibility?
    @NonNull public CardLargeHeaderView setImage(@Nullable Uri uri) {
        imageView.setVisibility(uri == null ? GONE : VISIBLE);
        imageView.loadImage(uri);
        return this;
    }

    @NonNull public CardLargeHeaderView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public CardLargeHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(String.format(getResources().getString(R.string.view_continue_reading_card_subtitle_read_date), subtitle));
        return this;
    }

    @NonNull public CardLargeHeaderView onClickListener(@Nullable OnClickListener listener) {
        backgroundView.setOnClickListener(listener);
        return this;
    }

    private void resetBackgroundColor() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.base20));
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(final int bmpHeight, @Nullable final PointF faceLocation, @ColorInt final int mainColor) {
            post(() -> {
                if (!ViewCompat.isAttachedToWindow(CardLargeHeaderView.this)) {
                    return;
                }
                animateBackgroundColor(CardLargeHeaderView.this, mainColor);
                if (faceLocation != null) {
                    imageView.getHierarchy().setActualImageFocusPoint(faceLocation);
                }
            });
        }

        @Override
        public void onImageFailed() {
            resetBackgroundColor();
        }

        private void animateBackgroundColor(@NonNull View view, @ColorInt int targetColor) {
            final int animDuration = 500;
            ObjectAnimator animator = ObjectAnimator.ofInt(view, "backgroundColor",
                    ContextCompat.getColor(getContext(), R.color.base20),
                    targetColor);
            animator.setEvaluator(new ArgbEvaluator());
            animator.setDuration(animDuration);
            animator.setupStartValues();
            animator.start();
        }
    }
}

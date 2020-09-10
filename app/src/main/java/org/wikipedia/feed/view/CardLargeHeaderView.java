package org.wikipedia.feed.view;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends ConstraintLayout {
    @BindView(R.id.view_card_header_large_background)
    View backgroundView;
    @BindView(R.id.view_card_header_large_image)
    FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_card_header_large_title)
    TextView titleView;
    @BindView(R.id.view_card_header_large_subtitle)
    TextView subtitleView;

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
        imageView.loadImage(uri, true);
        return this;
    }

    @NonNull
    public CardLargeHeaderView setTitle(@Nullable String title) {
        titleView.setText(StringUtil.fromHtml(title));
        return this;
    }

    @NonNull
    public CardLargeHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(getResources().getString(R.string.view_continue_reading_card_subtitle_read_date, subtitle));
        return this;
    }

    private void resetBackgroundColor() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.base20));
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

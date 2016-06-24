package org.wikipedia.feed.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardHeaderView extends LinearLayout {
    @BindView(R.id.view_card_header_image) AppCompatImageView imageView;
    @BindView(R.id.view_card_header_title) TextView titleView;
    @BindView(R.id.view_card_header_subtitle) TextView subtitleView;

    public CardHeaderView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_card_header, this);
        ButterKnife.bind(this);
    }

    @NonNull public CardHeaderView setImage(@DrawableRes int resId) {
        imageView.setImageResource(resId);
        return this;
    }

    @NonNull public CardHeaderView setImageCircleColor(@ColorRes int color) {
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{getResources().getColor(color)}
        );
        imageView.setSupportBackgroundTintList(colorStateList);
        return this;
    }

    @NonNull public CardHeaderView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public CardHeaderView setTitle(@StringRes int id) {
        titleView.setText(id);
        return this;
    }

    @NonNull public CardHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(subtitle);
        return this;
    }
}
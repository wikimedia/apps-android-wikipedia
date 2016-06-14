package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardHeaderView extends LinearLayout {
    @BindView(R.id.view_card_header_image) ImageView imageView;
    @BindView(R.id.view_card_header_title) TextView titleView;
    @BindView(R.id.view_card_header_subtitle) TextView subtitleView;

    public CardHeaderView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_card_header, this);
        ButterKnife.bind(this);
    }

    @NonNull public CardHeaderView setImage(@NonNull Uri uri) {
        imageView.setImageURI(uri);
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
package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.views.GoneIfEmptyTextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardLargeHeaderView extends LinearLayout {
    @BindView(R.id.view_card_header_large_image) SimpleDraweeView imageView;
    @BindView(R.id.view_card_header_large_title) TextView titleView;
    @BindView(R.id.view_card_header_large_page_title) TextView pageTitleView;
    @BindView(R.id.view_card_header_large_subtitle) GoneIfEmptyTextView subtitleView;

    public CardLargeHeaderView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_card_header_large, this);
        ButterKnife.bind(this);
    }

    @NonNull public CardLargeHeaderView setImage(@Nullable Uri uri) {
        imageView.setImageURI(uri);
        return this;
    }

    @NonNull public CardLargeHeaderView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public CardLargeHeaderView setTitle(@StringRes int id) {
        titleView.setText(id);
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
}
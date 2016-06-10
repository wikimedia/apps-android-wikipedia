package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ListCardItemView extends RelativeLayout {
    @BindView(R.id.view_list_card_item_image) SimpleDraweeView imageView;
    @BindView(R.id.view_list_card_item_title) TextView titleView;
    @BindView(R.id.view_list_card_item_subtitle) TextView subtitleView;

    public ListCardItemView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_list_card_item, this);
        ButterKnife.bind(this);
    }

    @NonNull public ListCardItemView setImage(@Nullable Uri uri) {
        imageView.setImageURI(uri);
        return this;
    }

    @NonNull public ListCardItemView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public ListCardItemView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(subtitle);
        return this;
    }
}
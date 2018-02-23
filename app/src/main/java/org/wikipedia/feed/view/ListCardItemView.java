package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ListCardItemView extends FrameLayout {
    public interface Callback {
        void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry);
        void onAddPageToList(@NonNull HistoryEntry entry);
        void onRemovePageFromList(@NonNull HistoryEntry entry);
        void onSharePage(@NonNull HistoryEntry entry);
    }

    @BindView(R.id.view_list_card_item_image) SimpleDraweeView imageView;
    @BindView(R.id.view_list_card_item_title) TextView titleView;
    @BindView(R.id.view_list_card_item_subtitle) GoneIfEmptyTextView subtitleView;

    @Nullable private Card card;
    @Nullable private Callback callback;
    @Nullable private HistoryEntry entry;

    public ListCardItemView(Context context) {
        super(context);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        inflate(getContext(), R.layout.view_list_card_item, this);
        ButterKnife.bind(this);
    }

    @NonNull public ListCardItemView setCard(@Nullable Card card) {
        this.card = card;
        return this;
    }

    @NonNull public ListCardItemView setCallback(@Nullable Callback callback) {
        this.callback = callback;
        return this;
    }

    @NonNull public ListCardItemView setHistoryEntry(@NonNull HistoryEntry entry) {
        this.entry = entry;
        setTitle(entry.getTitle().getDisplayText());
        setSubtitle(entry.getTitle().getDescription());
        setImage(entry.getTitle().getThumbUrl());
        return this;
    }

    @OnClick void onClick(View view) {
        if (callback != null && entry != null && card != null) {
            callback.onSelectPage(card, entry);
        }
    }

    @OnClick(R.id.view_list_card_item_menu) void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_item, menu.getMenu());
        menu.setOnMenuItemClickListener(new CardItemMenuClickListener());
        menu.show();
    }

    @VisibleForTesting @Nullable Callback getCallback() {
        return callback;
    }

    @VisibleForTesting @Nullable HistoryEntry getHistoryEntry() {
        return entry;
    }

    @VisibleForTesting void setImage(@Nullable String url) {
        ViewUtil.loadImageUrlInto(imageView, url);
    }

    @VisibleForTesting void setTitle(@Nullable CharSequence text) {
        titleView.setText(text);
    }

    @VisibleForTesting void setSubtitle(@Nullable CharSequence text) {
        subtitleView.setText(text != null ? StringUtils.capitalize(text.toString()) : null);
    }

    private class CardItemMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_feed_card_item_save:
                    if (callback != null && entry != null) {
                        callback.onAddPageToList(entry);
                        return true;
                    }
                    break;
                case R.id.menu_feed_card_item_share:
                    if (callback != null && entry != null) {
                        callback.onSharePage(entry);
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}

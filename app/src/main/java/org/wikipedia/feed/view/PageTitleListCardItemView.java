package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.history.HistoryEntry;

public class PageTitleListCardItemView extends ListCardItemView {
    @Nullable private FeedViewCallback callback;
    @Nullable private HistoryEntry entry;

    public PageTitleListCardItemView(Context context) {
        super(context);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null && entry != null) {
                    callback.onSelectPage(entry);
                }
            }
        });
        menuView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflowMenu(v);
            }
        });
    }

    @NonNull public PageTitleListCardItemView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    @NonNull public PageTitleListCardItemView setHistoryEntry(@NonNull HistoryEntry entry) {
        this.entry = entry;
        titleView.setText(entry.getTitle().getDisplayText());
        subtitleView.setText(entry.getTitle().getDescription());
        imageView.setImageURI(TextUtils.isEmpty(entry.getTitle().getThumbUrl()) ? null : Uri.parse(entry.getTitle().getThumbUrl()));
        return this;
    }

    private void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_item, menu.getMenu());
        menu.setOnMenuItemClickListener(new CardMenuClickListener());
        menu.show();
    }

    private class CardMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_feed_card_item_save:
                    if (callback != null && entry != null) {
                        callback.onAddPageToList(entry);
                    }
                    break;
                case R.id.menu_feed_card_item_share:
                    if (callback != null && entry != null) {
                        callback.onSharePage(entry);
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
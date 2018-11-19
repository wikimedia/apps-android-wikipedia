package org.wikipedia.feed.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ListCardItemView extends ConstraintLayout {
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
        inflate(getContext(), R.layout.view_list_card_item, this);
        ButterKnife.bind(this);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final int topBottomPadding = 16;
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding), 0, DimenUtil.roundedDpToPx(topBottomPadding));
        setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForeground(ContextCompat.getDrawable(getContext(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground)));
        }
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
        PageAvailableOfflineHandler.INSTANCE.check(entry.getTitle(), available -> setViewsGreyedOut(!available));
        return this;
    }

    @OnClick void onClick(View view) {
        if (callback != null && entry != null && card != null) {
            callback.onSelectPage(card, entry);
        }
    }

    @OnClick(R.id.view_list_card_item_menu) void showOverflowMenu(View anchorView) {
        new ReadingListBookmarkMenu(anchorView, true, new ReadingListBookmarkMenu.Callback() {
            @Override
            public void onAddRequest(@Nullable ReadingListPage page) {
                if (getCallback() != null && entry != null) {
                    getCallback().onAddPageToList(entry);
                }
            }

            @Override
            public void onDeleted(@Nullable ReadingListPage page) {
                if (getCallback() != null && entry != null) {
                    getCallback().onRemovePageFromList(entry);
                }
            }

            @Override
            public void onShare() {
                if (getCallback() != null && entry != null) {
                    getCallback().onSharePage(entry);
                }
            }
        }).show(entry.getTitle());
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

    @SuppressWarnings("checkstyle:magicnumber")
    private void setViewsGreyedOut(boolean greyedOut) {
        // Cannot use isAttachedToWindow() because the first two item will be reset when the setHistoryEntry() getting called even they are not visible.
        if (titleView == null || subtitleView == null || imageView == null) {
            return;
        }
        final float alpha = greyedOut ? 0.5f : 1.0f;
        titleView.setAlpha(alpha);
        subtitleView.setAlpha(alpha);
        imageView.setAlpha(alpha);
    }
}

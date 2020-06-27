package org.wikipedia.feed.view;

import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewListCardItemBinding;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

public class ListCardItemView extends ConstraintLayout {

    public interface Callback {
        void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry);
        void onAddPageToList(@NonNull HistoryEntry entry);
        void onMovePageToList(long sourceReadingListId, @NonNull HistoryEntry entry);
        void onRemovePageFromList(@NonNull HistoryEntry entry);
        void onSharePage(@NonNull HistoryEntry entry);
    }

    private ImageView imageView;
    private TextView titleView;
    private GoneIfEmptyTextView subtitleView;

    @Nullable private Card card;
    @Nullable private Callback callback;
    @Nullable private HistoryEntry entry;

    public ListCardItemView(Context context) {
        super(context);

        final ViewListCardItemBinding binding = ViewListCardItemBinding.bind(this);
        imageView = binding.viewListCardItemImage;
        titleView = binding.viewListCardItemTitle;
        subtitleView = binding.viewListCardItemSubtitle;

        setOnClickListener(v -> {
            if (callback != null && entry != null && card != null) {
                callback.onSelectPage(card, entry);
            }
        });
        setOnLongClickListener(v -> {
            new ReadingListBookmarkMenu(v, true, new ReadingListBookmarkMenu.Callback() {
                @Override
                public void onAddRequest(@Nullable ReadingListPage page) {
                    if (getCallback() != null && entry != null) {
                        getCallback().onAddPageToList(entry);
                    }
                }

                @Override
                public void onMoveRequest(@Nullable ReadingListPage page) {
                    if (getCallback() != null && entry != null) {
                        getCallback().onMovePageToList(page.listId(), entry);
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
            return false;
        });
        setFocusable(true);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final int topBottomPadding = 16;
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding), 0, DimenUtil.roundedDpToPx(topBottomPadding));
        setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForeground(AppCompatResources.getDrawable(getContext(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground)));
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
        setTitle(StringUtil.fromHtml(entry.getTitle().getDisplayText()));
        setSubtitle(entry.getTitle().getDescription());
        setImage(entry.getTitle().getThumbUrl());
        PageAvailableOfflineHandler.INSTANCE.check(entry.getTitle(), available -> setViewsGreyedOut(!available));
        return this;
    }

    @VisibleForTesting @Nullable Callback getCallback() {
        return callback;
    }

    @VisibleForTesting @Nullable HistoryEntry getHistoryEntry() {
        return entry;
    }

    @VisibleForTesting void setImage(@Nullable String url) {
        if (url == null) {
            imageView.setVisibility(GONE);
        } else {
            imageView.setVisibility(VISIBLE);
            ViewUtil.loadImageWithRoundedCorners(imageView, url);
        }
    }

    @VisibleForTesting void setTitle(@Nullable CharSequence text) {
        titleView.setText(text);
    }

    @VisibleForTesting void setSubtitle(@Nullable CharSequence text) {
        subtitleView.setText(text);
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

package org.wikipedia.feed.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.mostread.MostReadArticles;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.readinglist.LongPressMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.TransitionUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.GraphView;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class ListCardItemView extends ConstraintLayout {

    public interface Callback {
        void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry, boolean openInNewBackgroundTab);
        void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry, @NonNull Pair<View, String>[] sharedElements);
        void onAddPageToList(@NonNull HistoryEntry entry, boolean addToDefault);
        void onMovePageToList(long sourceReadingListId, @NonNull HistoryEntry entry);
    }

    @BindView(R.id.view_list_card_number) GradientCircleNumberView numberView;
    @BindView(R.id.view_list_card_item_image) ImageView imageView;
    @BindView(R.id.view_list_card_item_title) TextView titleView;
    @BindView(R.id.view_list_card_item_subtitle) GoneIfEmptyTextView subtitleView;
    @BindView(R.id.view_list_card_item_pageviews) TextView pageViewsView;
    @BindView(R.id.view_list_card_item_graph) GraphView graphView;

    @Nullable private Card card;
    @Nullable private Callback callback;
    @Nullable private HistoryEntry entry;
    private static final int DEFAULT_VIEW_HISTORY_ITEMS = 5;

    public ListCardItemView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_list_card_item, this);
        ButterKnife.bind(this);

        setFocusable(true);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final int topBottomPadding = 16;
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding), 0, DimenUtil.roundedDpToPx(topBottomPadding));
        DeviceUtil.setContextClickAsLongClick(this);
        setBackground(AppCompatResources.getDrawable(getContext(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground)));
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

    @OnClick void onClick(View view) {
        if (callback != null && entry != null && card != null) {
            callback.onSelectPage(card, entry, TransitionUtil.getSharedElements(getContext(), imageView));
        }
    }

    @OnLongClick boolean onLongClick(View view) {
        new LongPressMenu(view, true, new LongPressMenu.Callback() {
            @Override
            public void onOpenLink(@NonNull HistoryEntry entry) {
                if (getCallback() != null && card != null) {
                    getCallback().onSelectPage(card, entry, false);
                }
            }

            @Override
            public void onOpenInNewTab(@NonNull HistoryEntry entry) {
                if (getCallback() != null && card != null) {
                    getCallback().onSelectPage(card, entry, true);
                }
            }

            @Override
            public void onAddRequest(@NonNull HistoryEntry entry, boolean addToDefault) {
                if (getCallback() != null) {
                    getCallback().onAddPageToList(entry, addToDefault);
                }
            }

            @Override
            public void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
                if (getCallback() != null) {
                    getCallback().onMovePageToList(page.getListId(), entry);
                }
            }
        }).show(entry);
        return false;
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
            ViewUtil.loadImageWithRoundedCorners(imageView, url, true);
        }
    }

    @VisibleForTesting void setTitle(@Nullable CharSequence text) {
        titleView.setText(text);
    }

    @VisibleForTesting void setSubtitle(@Nullable CharSequence text) {
        subtitleView.setText(text);
    }

    public void setNumber(int number) {
        numberView.setVisibility(VISIBLE);
        numberView.setNumber(number);
    }

    public void setPageViews(int pageViews) {
        pageViewsView.setVisibility(VISIBLE);
        pageViewsView.setText(getPageViewText(pageViews));
    }

    public void setGraphView(@NonNull List<MostReadArticles.ViewHistory> viewHistories) {
        List<Float> dataSet = new ArrayList<>();

        int i = viewHistories.size();
        while (DEFAULT_VIEW_HISTORY_ITEMS > i++) {
            dataSet.add(0f);
        }

        for (MostReadArticles.ViewHistory viewHistory : viewHistories) {
            dataSet.add(viewHistory.getViews());
        }
        graphView.setVisibility(VISIBLE);
        graphView.setData(dataSet);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private String getPageViewText(int pageViews) {
        if (pageViews < 1000) {
            return String.valueOf(pageViews);
        } else if (pageViews < 1000000) {
            return getContext().getString(R.string.view_top_read_card_pageviews_k_suffix, Math.round(pageViews / 1000f));
        } else {
            return getContext().getString(R.string.view_top_read_card_pageviews_m_suffix, Math.round(pageViews / 1000000f));
        }
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

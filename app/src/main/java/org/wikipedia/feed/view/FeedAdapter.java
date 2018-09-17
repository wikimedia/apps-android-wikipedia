package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.feed.FeedCoordinatorBase;
import org.wikipedia.feed.announcement.AnnouncementCardView;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView;
import org.wikipedia.feed.dayheader.DayHeaderCardView;
import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.news.NewsListCardView;
import org.wikipedia.feed.offline.OfflineCard;
import org.wikipedia.feed.offline.OfflineCardView;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public class FeedAdapter<T extends View & FeedCardView<?>> extends DefaultRecyclerAdapter<Card, T> {
    public interface Callback extends ItemTouchHelperSwipeAdapter.Callback,
            ListCardItemView.Callback, CardHeaderView.Callback,  FeaturedImageCardView.Callback,
            SearchCardView.Callback, NewsListCardView.Callback, AnnouncementCardView.Callback,
            RandomCardView.Callback, ListCardView.Callback, BecauseYouReadCardView.Callback {
        void onShowCard(@Nullable Card card);
        void onRequestMore();
        void onRetryFromOffline();
        void onError(@NonNull Throwable t);
    }

    @NonNull private FeedCoordinatorBase coordinator;
    @Nullable private FeedView feedView;
    @Nullable private Callback callback;
    private Card lastCardReloadTrigger = null;

    public FeedAdapter(@NonNull FeedCoordinatorBase coordinator, @Nullable Callback callback) {
        super(coordinator.getCards());
        this.coordinator = coordinator;
        this.callback = callback;
    }

    @NonNull
    @Override public DefaultViewHolder<T> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DefaultViewHolder<>(newView(parent.getContext(), viewType));
    }

    @Override public void onBindViewHolder(@NonNull DefaultViewHolder<T> holder, int position) {
        Card item = item(position);
        T view = holder.getView();

        if (coordinator.finished()
                && position == getItemCount() - 1
                && !(item instanceof OfflineCard)
                && item != lastCardReloadTrigger
                && callback != null) {
            callback.onRequestMore();
            lastCardReloadTrigger = item;
        } else {
            lastCardReloadTrigger = null;
        }

        //noinspection unchecked
        ((FeedCardView<Card>) view).setCard(item);

        if (view instanceof OfflineCardView && position == 1) {
            ((OfflineCardView) view).setTopPadding();
        }
    }

    @Override public void onViewAttachedToWindow(@NonNull DefaultViewHolder<T> holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getView() instanceof SearchCardView) {
            adjustSearchView((SearchCardView) holder.getView());
        } else if (holder.getView() instanceof DayHeaderCardView) {
            adjustDayHeaderView((DayHeaderCardView) holder.getView());
        }
        holder.getView().setCallback(callback);
        if (callback != null) {
            callback.onShowCard(holder.getView().getCard());
        }
    }

    @Override public void onViewDetachedFromWindow(@NonNull DefaultViewHolder<T> holder) {
        holder.getView().setCallback(null);
        super.onViewDetachedFromWindow(holder);
    }

    @Override public int getItemViewType(int position) {
        return item(position).type().code();
    }

    @NonNull private T newView(@NonNull Context context, int viewType) {
        //noinspection unchecked
        return (T) CardType.of(viewType).newView(context);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.feedView = (FeedView) recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.feedView = null;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void adjustSearchView(@NonNull SearchCardView view) {
        StaggeredGridLayoutManager.LayoutParams layoutParams
                = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.setFullSpan(true);
        final int bottomMargin = 8;
        layoutParams.bottomMargin = DimenUtil.roundedDpToPx(bottomMargin);

        if (feedView != null && feedView.getColumns() > 1) {
            layoutParams.leftMargin = ((View) view.getParent()).getWidth() / 6;
            layoutParams.rightMargin = layoutParams.leftMargin;
        }
        view.setLayoutParams(layoutParams);
    }

    private void adjustDayHeaderView(@NonNull DayHeaderCardView view) {
        StaggeredGridLayoutManager.LayoutParams layoutParams
                = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.setFullSpan(true);
    }
}

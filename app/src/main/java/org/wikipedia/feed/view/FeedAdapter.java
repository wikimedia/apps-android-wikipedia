package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.feed.FeedCoordinatorBase;
import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.news.NewsListCardView;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public class FeedAdapter<T extends View & FeedCardView<?>> extends DefaultRecyclerAdapter<Card, T> {
    public interface Callback extends ItemTouchHelperSwipeAdapter.Callback,
            PageTitleListCardItemView.Callback, CardHeaderView.Callback,
            FeaturedImageCardView.Callback, SearchCardView.Callback, NewsListCardView.Callback {
        void onRequestMore();
    }

    @NonNull private FeedCoordinatorBase coordinator;
    @Nullable private Callback callback;

    public FeedAdapter(@NonNull FeedCoordinatorBase coordinator, @Nullable Callback callback) {
        super(coordinator.getCards());
        this.coordinator = coordinator;
        this.callback = callback;
    }

    @Override public DefaultViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DefaultViewHolder<>(newView(parent.getContext(), viewType));
    }

    @Override public void onBindViewHolder(DefaultViewHolder<T> holder, int position) {
        Card item = item(position);
        T view = holder.getView();

        if (coordinator.finished()
                && position == getItemCount() - 1
                && callback != null) {
            callback.onRequestMore();
        }

        if (isCardAssociatedWithView(view, item)) {
            // Don't bother reloading the same card into the same view
            return;
        }
        associateCardWithView(view, item);

        //noinspection unchecked
        ((FeedCardView<Card>) view).setCard(item);
    }

    @Override public void onViewAttachedToWindow(DefaultViewHolder<T> holder) {
        super.onViewAttachedToWindow(holder);
        holder.getView().setCallback(callback);
    }

    @Override public void onViewDetachedFromWindow(DefaultViewHolder<T> holder) {
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

    private boolean isCardAssociatedWithView(@NonNull View view, @NonNull Card card) {
        return card.equals(view.getTag());
    }

    private void associateCardWithView(@NonNull View view, @NonNull Card card) {
        view.setTag(card);
    }
}
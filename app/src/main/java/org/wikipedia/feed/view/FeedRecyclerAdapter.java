package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.ViewGroup;

import org.wikipedia.feed.FeedCoordinatorBase;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCard;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView;
import org.wikipedia.feed.continuereading.ContinueReadingCard;
import org.wikipedia.feed.continuereading.ContinueReadingCardView;
import org.wikipedia.feed.demo.IntegerListCard;
import org.wikipedia.feed.demo.IntegerListCardView;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.featured.FeaturedArticleCardView;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.mostread.MostReadCardView;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.random.RandomCard;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.searchbar.SearchCard;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

public class FeedRecyclerAdapter extends DefaultRecyclerAdapter<Card, CardView> {
    private static final int VIEW_TYPE_SEARCH_BAR = 0;
    private static final int VIEW_TYPE_CONTINUE_READING = 1;
    private static final int VIEW_TYPE_BECAUSE_YOU_READ = 2;
    private static final int VIEW_TYPE_MOST_READ = 3;
    private static final int VIEW_TYPE_FEATURED_ARTICLE = 4;
    private static final int VIEW_TYPE_RANDOM = 5;
    private static final int VIEW_TYPE_INTEGER_LIST = 100;

    @NonNull private FeedCoordinatorBase coordinator;
    @Nullable private FeedViewCallback callback;

    public FeedRecyclerAdapter(@NonNull FeedCoordinatorBase coordinator, @Nullable FeedViewCallback callback) {
        super(coordinator.getCards());
        this.coordinator = coordinator;
        this.callback = callback;
    }

    @Override public DefaultViewHolder<CardView> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DefaultViewHolder<>(newView(parent.getContext(), viewType));
    }

    @Override public void onBindViewHolder(DefaultViewHolder<CardView> holder, int position) {
        Card item = item(position);
        CardView view = holder.getView();

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

        if (view instanceof IntegerListCardView) {
            ((IntegerListCardView) view).set((IntegerListCard) item);
        } else if (view instanceof ContinueReadingCardView) {
            ((ContinueReadingCardView) view).set((ContinueReadingCard) item);
        } else if (view instanceof BecauseYouReadCardView) {
            ((BecauseYouReadCardView) view).set((BecauseYouReadCard) item);
        } else if (view instanceof SearchCardView) {
            ((SearchCardView) view).set((SearchCard) item);
        } else if (view instanceof MostReadCardView) {
            ((MostReadCardView) view).set((MostReadListCard) item);
        } else if (view instanceof FeaturedArticleCardView) {
            ((FeaturedArticleCardView) view).set((FeaturedArticleCard) item);
        } else if (view instanceof RandomCardView) {
            ((RandomCardView) view).set((RandomCard) item);
        } else {
            throw new IllegalStateException("Unknown type=" + view.getClass());
        }
    }

    @Override public int getItemViewType(int position) {
        Card item = item(position);
        if (item instanceof IntegerListCard) {
            return VIEW_TYPE_INTEGER_LIST;
        } else if (item instanceof ContinueReadingCard) {
            return VIEW_TYPE_CONTINUE_READING;
        } else if (item instanceof BecauseYouReadCard) {
            return VIEW_TYPE_BECAUSE_YOU_READ;
        } else if (item instanceof SearchCard) {
            return VIEW_TYPE_SEARCH_BAR;
        } else if (item instanceof MostReadListCard) {
            return VIEW_TYPE_MOST_READ;
        } else if (item instanceof FeaturedArticleCard) {
            return VIEW_TYPE_FEATURED_ARTICLE;
        } else if (item instanceof RandomCard) {
            return VIEW_TYPE_RANDOM;
        } else {
            throw new IllegalStateException("Unknown type=" + item.getClass());
        }
    }

    @NonNull private CardView newView(@NonNull Context context, int viewType) {
        switch(viewType) {
            case VIEW_TYPE_INTEGER_LIST:
                return new IntegerListCardView(context);
            case VIEW_TYPE_CONTINUE_READING:
                return new ContinueReadingCardView(context).setCallback(callback);
            case VIEW_TYPE_BECAUSE_YOU_READ:
                return new BecauseYouReadCardView(context).setCallback(callback);
            case VIEW_TYPE_SEARCH_BAR:
                return new SearchCardView(context).setCallback(callback);
            case VIEW_TYPE_MOST_READ:
                return new MostReadCardView(context).setCallback(callback);
            case VIEW_TYPE_FEATURED_ARTICLE:
                return new FeaturedArticleCardView(context).setCallback(callback);
            case VIEW_TYPE_RANDOM:
                return new RandomCardView(context);
            default:
                throw new IllegalArgumentException("viewType=" + viewType);
        }
    }

    private boolean isCardAssociatedWithView(@NonNull CardView view, @NonNull Card card) {
        return card.equals(view.getTag());
    }

    private void associateCardWithView(@NonNull CardView view, @NonNull Card card) {
        view.setTag(card);
    }
}

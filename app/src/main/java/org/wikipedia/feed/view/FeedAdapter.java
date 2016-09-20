package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.feed.FeedCoordinatorBase;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCard;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView;
import org.wikipedia.feed.continuereading.ContinueReadingCard;
import org.wikipedia.feed.continuereading.ContinueReadingCardView;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.featured.FeaturedArticleCardView;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.image.FeaturedImageCardView;
import org.wikipedia.feed.mainpage.MainPageCard;
import org.wikipedia.feed.mainpage.MainPageCardView;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.mostread.MostReadCardView;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsListCard;
import org.wikipedia.feed.news.NewsListCardView;
import org.wikipedia.feed.random.RandomCard;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.searchbar.SearchCard;
import org.wikipedia.feed.searchbar.SearchCardView;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

public class FeedAdapter extends DefaultRecyclerAdapter<Card, View> {
    @NonNull private FeedCoordinatorBase coordinator;
    @Nullable private FeedViewCallback callback;

    public FeedAdapter(@NonNull FeedCoordinatorBase coordinator, @Nullable FeedViewCallback callback) {
        super(coordinator.getCards());
        this.coordinator = coordinator;
        this.callback = callback;
    }

    @Override public DefaultViewHolder<View> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DefaultViewHolder<>(newView(parent.getContext(), viewType));
    }

    @Override public void onBindViewHolder(DefaultViewHolder<View> holder, int position) {
        Card item = item(position);
        View view = holder.getView();

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

        if (view instanceof ContinueReadingCardView) {
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
        } else if (view instanceof NewsListCardView) {
            ((NewsListCardView) view).set((NewsListCard) item);
        } else if (view instanceof MainPageCardView) {
            ((MainPageCardView) view).set((MainPageCard) item);
        } else if (view instanceof FeaturedImageCardView) {
            ((FeaturedImageCardView) view).set((FeaturedImageCard) item);
        }
    }

    @Override public int getItemViewType(int position) {
        return item(position).type().code();
    }

    @NonNull private View newView(@NonNull Context context, int viewType) {
        View view = CardType.of(viewType).newView(context);
        if (view instanceof FeedCardView) {
            ((FeedCardView) view).setCallback(callback);
        } else if (view instanceof RandomCardView) {
            ((RandomCardView) view).setCallback(callback);
        } else if (view instanceof MainPageCardView) {
            ((MainPageCardView) view).setCallback(callback);
        }
        return view;
    }

    private boolean isCardAssociatedWithView(@NonNull View view, @NonNull Card card) {
        return card.equals(view.getTag());
    }

    private void associateCardWithView(@NonNull View view, @NonNull Card card) {
        view.setTag(card);
    }
}

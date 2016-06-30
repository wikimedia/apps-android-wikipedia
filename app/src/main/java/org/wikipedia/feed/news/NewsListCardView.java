package org.wikipedia.feed.news;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.HorizontalScrollingListCardItemView;
import org.wikipedia.feed.view.HorizontalScrollingListCardView;
import org.wikipedia.util.DateUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class NewsListCardView extends HorizontalScrollingListCardView
        implements ItemTouchHelperSwipeAdapter.SwipeableView {

    public NewsListCardView(@NonNull Context context) {
        super(context);
    }

    public void set(@NonNull NewsListCard card) {
        header(card);
        footer();
        set(new RecyclerAdapter(card.items(), getCallback()));
    }

    private void header(@NonNull NewsListCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(R.string.view_card_news_title)
                .setSubtitle(DateUtil.getFeedCardDateString(card.age().baseCalendar()))
                .setImage(R.drawable.icon_in_the_news)
                .setImageCircleColor(R.color.gray_disabled)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
    }

    private void footer() {
        CardFooterView footer = new CardFooterView(getContext())
                .setText(getResources().getString(R.string.view_card_news_footer_text).toUpperCase());
        footer(footer);
    }

    private static class RecyclerAdapter extends HorizontalScrollingListCardView.RecyclerAdapter<NewsItemCard> {
        @Nullable private FeedViewCallback callback;

        RecyclerAdapter(@NonNull List<NewsItemCard> items, @Nullable FeedViewCallback callback) {
            super(items);
            this.callback = callback;
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<HorizontalScrollingListCardItemView> holder, int i) {
            NewsItemCard card = item(i);
            holder.getView().setText(card.text());
            holder.getView().setImage(card.image());
            holder.getView().setCallback(callback);
        }
    }
}

package org.wikipedia.feed.news;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.HorizontalScrollingListCardItemView;
import org.wikipedia.feed.view.HorizontalScrollingListCardView;
import org.wikipedia.util.DateUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class NewsListCardView extends HorizontalScrollingListCardView<NewsListCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onNewsItemSelected(@NonNull NewsItemCard card, @NonNull HorizontalScrollingListCardItemView view);
    }

    public NewsListCardView(@NonNull Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull NewsListCard card) {
        super.setCard(card);
        header(card);
        set(new RecyclerAdapter(card.items()));
    }

    private void header(@NonNull NewsListCard card) {
        headerView().setTitle(R.string.view_card_news_title)
                .setSubtitle(DateUtil.getFeedCardDateString(card.date().baseCalendar()))
                .setImage(R.drawable.icon_in_the_news)
                .setImageCircleColor(R.color.base50)
                .setCard(card)
                .setCallback(getCallback());
    }

    private class RecyclerAdapter extends HorizontalScrollingListCardView.RecyclerAdapter<NewsItemCard> {
        RecyclerAdapter(@NonNull List<NewsItemCard> items) {
            super(items);
        }

        @Nullable @Override protected FeedAdapter.Callback callback() {
            return getCallback();
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<HorizontalScrollingListCardItemView> holder, int i) {
            final NewsItemCard card = item(i);
            holder.getView().setText(card.text());
            holder.getView().setImage(card.image());
            holder.getView().setOnClickListener((view) -> {
                if (getCallback() != null) {
                    getCallback().onNewsItemSelected(card, (HorizontalScrollingListCardItemView) view);
                }
            });
        }
    }
}

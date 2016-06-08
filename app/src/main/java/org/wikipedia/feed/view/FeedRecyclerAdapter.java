package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.ViewGroup;

import org.wikipedia.feed.becauseyouread.BecauseYouReadCard;
import org.wikipedia.feed.becauseyouread.BecauseYouReadCardView;
import org.wikipedia.feed.continuereading.ContinueReadingCard;
import org.wikipedia.feed.continuereading.ContinueReadingCardView;
import org.wikipedia.feed.demo.IntegerListCard;
import org.wikipedia.feed.demo.IntegerListCardView;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class FeedRecyclerAdapter extends DefaultRecyclerAdapter<Card, CardView> {
    private static final int VIEW_TYPE_INTEGER_LIST = 0;
    private static final int VIEW_TYPE_CONTINUE_READING = 1;
    private static final int VIEW_TYPE_BECAUSE_YOU_READ = 2;

    public FeedRecyclerAdapter(@NonNull List<Card> items) {
        super(items);
    }

    @Override public DefaultViewHolder<CardView> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DefaultViewHolder<>(newView(parent.getContext(), viewType));
    }

    @Override public void onBindViewHolder(DefaultViewHolder<CardView> holder, int position) {
        Card item = item(position);
        CardView view = holder.getView();

        if (view instanceof IntegerListCardView) {
            ((IntegerListCardView) view).set((IntegerListCard) item);
        } else if (view instanceof ContinueReadingCardView) {
            ((ContinueReadingCardView) view).set((ContinueReadingCard) item);
        } else if (view instanceof BecauseYouReadCardView) {
            ((BecauseYouReadCardView) view).set((BecauseYouReadCard) item);
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
        } else {
            throw new IllegalStateException("Unknown type=" + item.getClass());
        }
    }

    @NonNull private CardView newView(@NonNull Context context, int viewType) {
        switch(viewType) {
            case VIEW_TYPE_INTEGER_LIST:
                return new IntegerListCardView(context);
            case VIEW_TYPE_CONTINUE_READING:
                return new ContinueReadingCardView(context);
            case VIEW_TYPE_BECAUSE_YOU_READ:
                return new BecauseYouReadCardView(context);
            default:
                throw new IllegalArgumentException("viewType=" + viewType);
        }
    }
}
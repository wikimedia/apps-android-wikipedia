package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;

import java.util.List;

public abstract class HorizontalScrollingListCardView<T extends Card> extends ListCardView<T> {
    public HorizontalScrollingListCardView(@NonNull Context context) {
        super(context);
    }

    @Override protected void initRecycler(@NonNull RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        recyclerView.addOnItemTouchListener(new DontInterceptTouchListener());
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setClipToPadding(false);
        MarginLayoutParams params = (MarginLayoutParams) recyclerView.getLayoutParams();
        final int height = DimenUtil.roundedDpToPx(228);
        params.height = height;
        final int padding = DimenUtil.roundedDpToPx(12);
        recyclerView.setPadding(padding, 0, padding, 0);
    }

    protected abstract static class RecyclerAdapter<T>
            extends DefaultRecyclerAdapter<T, HorizontalScrollingListCardItemView> {
        protected RecyclerAdapter(@NonNull List<T> items) {
            super(items);
        }

        @Nullable protected abstract FeedAdapter.Callback callback();

        @Override public DefaultViewHolder<HorizontalScrollingListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                                         int viewType) {
            return new DefaultViewHolder<>(new HorizontalScrollingListCardItemView(parent.getContext()));
        }

        @Override public void onViewAttachedToWindow(DefaultViewHolder<HorizontalScrollingListCardItemView> holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(callback());
        }

        @Override public void onViewDetachedFromWindow(DefaultViewHolder<HorizontalScrollingListCardItemView> holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }
}

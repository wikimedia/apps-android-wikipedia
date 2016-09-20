package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DontInterceptTouchListener;
import org.wikipedia.views.MarginItemDecoration;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class HorizontalScrollingListCardView extends CardView {
    @BindView(R.id.view_horizontal_scrolling_list_card_header) View headerView;
    @BindView(R.id.view_horizontal_scrolling_list_card_list) RecyclerView recyclerView;

    @Nullable private FeedViewCallback callback;

    @Nullable
    public FeedViewCallback callback() {
        return callback;
    }

    public HorizontalScrollingListCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_horizontal_scrolling_list_card, this);
        ButterKnife.bind(this);
        initRecycler();
    }

    @NonNull public HorizontalScrollingListCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    protected void update() {
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    protected void header(@NonNull View view) {
        ViewUtil.replace(headerView, view);
        headerView = view;
    }

    @Nullable
    public FeedViewCallback getCallback() {
        return callback;
    }

    protected void set(@Nullable RecyclerAdapter<?> adapter) {
        recyclerView.setAdapter(adapter);
    }

    private void initRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_horizontal,
                R.dimen.view_horizontal_scrolling_list_card_item_margin_vertical));
        recyclerView.addOnItemTouchListener(new DontInterceptTouchListener());
        recyclerView.setNestedScrollingEnabled(false);
    }

    protected abstract static class RecyclerAdapter<T>
            extends DefaultRecyclerAdapter<T, HorizontalScrollingListCardItemView> {
        protected RecyclerAdapter(@NonNull List<T> items) {
            super(items);
        }

        @Override public DefaultViewHolder<HorizontalScrollingListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                                         int viewType) {
            return new DefaultViewHolder<>(new HorizontalScrollingListCardItemView(parent.getContext()));
        }
    }
}

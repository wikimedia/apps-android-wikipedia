package org.wikipedia.feed.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.DrawableItemDecoration;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public abstract class ListCardView<T extends Card> extends DefaultFeedCardView<T> {
    public interface Callback {
        void onMoreContentSelected(@NonNull Card card);
    }

    @BindView(R.id.view_list_card_header) CardHeaderView headerView;
    @BindView(R.id.view_list_card_large_header) CardLargeHeaderView largeHeaderView;
    @BindView(R.id.view_list_card_list) RecyclerView recyclerView;
    @BindView(R.id.view_list_card_more_container) View moreContentContainer;
    @BindView(R.id.view_list_card_more_text) TextView moreContentTextView;

    public ListCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_list_card, this);
        ButterKnife.bind(this);
        initRecycler(recyclerView);
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    protected void set(@Nullable RecyclerView.Adapter<?> adapter) {
        recyclerView.setAdapter(adapter);
    }

    protected void update() {
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    protected CardHeaderView headerView() {
        return headerView;
    }

    protected CardLargeHeaderView largeHeaderView() {
        return largeHeaderView;
    }

    protected View getLayoutDirectionView() {
        return recyclerView;
    }

    /** Called by the constructor. Override to provide custom behavior but otherwise do not call
        directly. */
    protected void initRecycler(@NonNull RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable, true));
        recyclerView.setNestedScrollingEnabled(false);
    }

    protected void setMoreContentTextView(@NonNull String text) {
        moreContentContainer.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
        moreContentTextView.setText(text);
    }

    @OnClick(R.id.view_list_card_more_container) void moreContentClicked() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onMoreContentSelected(getCard());
        }
    }
}

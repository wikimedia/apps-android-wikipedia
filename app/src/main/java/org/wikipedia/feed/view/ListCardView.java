package org.wikipedia.feed.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewListCardBinding;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.DrawableItemDecoration;

public abstract class ListCardView<T extends Card> extends DefaultFeedCardView<T> {
    public interface Callback {
        void onMoreContentSelected(@NonNull Card card);
    }

    private CardHeaderView headerView;
    private CardLargeHeaderView largeHeaderView;
    private RecyclerView recyclerView;
    private View moreContentContainer;
    private TextView moreContentTextView;

    public ListCardView(Context context) {
        super(context);

        final ViewListCardBinding binding = ViewListCardBinding.inflate(LayoutInflater.from(context));
        headerView = binding.viewListCardHeader;
        largeHeaderView = binding.viewListCardLargeHeader;
        recyclerView = binding.viewListCardList;
        moreContentContainer = binding.viewListCardMoreContainer;
        moreContentTextView = binding.viewListCardMoreText;

        moreContentContainer.setOnClickListener(v -> {
            if (getCallback() != null && getCard() != null) {
                getCallback().onMoreContentSelected(getCard());
            }
        });
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
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable));
        recyclerView.setNestedScrollingEnabled(false);
    }

    protected void setMoreContentTextView(@NonNull String text) {
        moreContentContainer.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
        moreContentTextView.setText(text);
    }
}

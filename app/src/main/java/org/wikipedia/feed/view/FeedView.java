package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;

import org.wikipedia.R;
import org.wikipedia.views.AutoFitRecyclerView;
import org.wikipedia.views.HeaderMarginItemDecoration;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.MarginItemDecoration;

public class FeedView extends AutoFitRecyclerView {
    private StaggeredGridLayoutManager recyclerLayoutManager;
    @Nullable private ItemTouchHelper itemTouchHelper;

    public FeedView(Context context) {
        super(context);
        init();
    }

    public FeedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void callback(@Nullable ItemTouchHelperSwipeAdapter.Callback callback) {
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
            itemTouchHelper = null;
        }

        if (callback != null) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperSwipeAdapter(callback));
            itemTouchHelper.attachToRecyclerView(this);
        }
    }

    public int getFirstVisibleItemPosition() {
        StaggeredGridLayoutManager manager = ((StaggeredGridLayoutManager) getLayoutManager());
        int[] visibleItems = new int[manager.getSpanCount()];
        manager.findFirstVisibleItemPositions(visibleItems);
        return visibleItems[0];
    }

    private void init() {
        setVerticalScrollBarEnabled(true);
        minColumnWidth((int) getResources().getDimension(R.dimen.view_feed_min_column_width));
        recyclerLayoutManager = new StaggeredGridLayoutManager(getColumns(),
                StaggeredGridLayoutManager.VERTICAL);
        setLayoutManager(recyclerLayoutManager);
        addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical,
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical));
        addItemDecoration(new HeaderMarginItemDecoration(getContext(),
                R.dimen.view_list_first_card_margin_top));
        callback(new RecyclerViewColumnCallback());
    }

    private class RecyclerViewColumnCallback implements AutoFitRecyclerView.Callback {
        @Override public void onColumns(int columns) {
            // todo: when there is only one element, should we setSpanCount to 1? e.g.:
            //       adapter.getItemCount() <= 1 ? 1 : columns;
            //       we would need to also notify the layout manager when the data set changes
            //       though.
            recyclerLayoutManager.setSpanCount(columns);
        }
    }
}

package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.AutoFitRecyclerView;
import org.wikipedia.views.MarginItemDecoration;

import java.util.Collections;
import java.util.List;

public class FeedView extends AutoFitRecyclerView {
    private StaggeredGridLayoutManager recyclerLayoutManager;
    private FeedRecyclerAdapter recyclerAdapter;

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

    public void set(@NonNull List<Card> cards) {
        // TODO: should this class be responsible for showing a "no items in collection" view? It
        //       would be nice to show placeholder elements while it loads.
        recyclerAdapter = new FeedRecyclerAdapter(cards);
        setAdapter(recyclerAdapter);
    }

    public void update() {
        recyclerAdapter.notifyDataSetChanged();
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
        callback(new RecyclerViewColumnCallback());
        set(Collections.<Card>emptyList());
    }

    private class RecyclerViewColumnCallback implements AutoFitRecyclerView.Callback {
        @Override public void onColumns(int columns) {
            // todo: when there is only one column, should we setSpanCount to 1? e.g.:
            //   recyclerAdapter.getItemCount() <= 1 ? 1 : columns;
            // We would need to also notify the layout manager when the data set changes though.
            recyclerLayoutManager.setSpanCount(columns);
        }
    }
}
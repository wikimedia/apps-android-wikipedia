package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.util.log.L;
import org.wikipedia.views.AutoFitRecyclerView;
import org.wikipedia.views.HeaderMarginItemDecoration;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;
import org.wikipedia.views.MarginItemDecoration;

import static org.wikipedia.util.DimenUtil.getDimension;
import static org.wikipedia.util.DimenUtil.roundedDpToPx;

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

    public void setCallback(@Nullable ItemTouchHelperSwipeAdapter.Callback callback) {
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(new DummyView(getContext()));
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
        recyclerLayoutManager = new StaggeredGridLayoutManager(getColumns(),
                StaggeredGridLayoutManager.VERTICAL);
        setLayoutManager(recyclerLayoutManager);
        addItemDecoration(new MarginItemDecoration(getContext(),
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical,
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical));
        addItemDecoration(new HeaderMarginItemDecoration(getContext(),
                R.dimen.view_feed_padding_top, R.dimen.view_feed_search_padding_bottom));
        setCallback(new RecyclerViewColumnCallback());
    }

    /* Workaround for https://code.google.com/p/android/issues/detail?id=205947.
       ItemTouchHelper.attachToRecyclerView(null) should remove its gesture callback before nulling
       its RecyclerView:
        java.lang.NullPointerException: Attempt to invoke virtual method 'android.view.View android.support.v7.widget.RecyclerView.findChildViewUnder(float, float)' on a null object reference
            at android.support.v7.widget.helper.ItemTouchHelper.findChildView(ItemTouchHelper.java:1024)
            at android.support.v7.widget.helper.ItemTouchHelper.access$2400(ItemTouchHelper.java:76)
            at android.support.v7.widget.helper.ItemTouchHelper$ItemTouchHelperGestureListener.onLongPress(ItemTouchHelper.java:2265)
            at android.view.GestureDetector.dispatchLongPress(GestureDetector.java:770)
            at android.view.GestureDetector.-wrap0(GestureDetector.java)
            at android.view.GestureDetector$GestureHandler.handleMessage(GestureDetector.java:293)
            at android.os.Handler.dispatchMessage(Handler.java:102)
            at android.os.Looper.loop(Looper.java:154)
            at android.app.ActivityThread.main(ActivityThread.java:6077)
            at java.lang.reflect.Method.invoke(Native Method)
            at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:865)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:755)
     */
    private static class DummyView extends RecyclerView {
        DummyView(Context context) {
            super(context);
        }

        @Override public View findChildViewUnder(float x, float y) {
            L.logRemoteError(new RemoteLogException("ItemTouchHelper.attachToRecyclerView(null)"));
            return super.findChildViewUnder(x, y);
        }
    }

    private class RecyclerViewColumnCallback implements AutoFitRecyclerView.Callback {
        @Override public void onColumns(int columns) {
            // todo: when there is only one element, should we setSpanCount to 1? e.g.:
            //       adapter.getItemCount() <= 1 ? 1 : columns;
            //       we would need to also notify the layout manager when the data set changes
            //       though.
            recyclerLayoutManager.setSpanCount(columns);
            int padding = roundedDpToPx(getDimension(R.dimen.view_list_card_margin_horizontal));
            setPadding(padding, 0, padding, 0);
        }
    }
}

package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

public class NavMenuAwareRecyclerView extends RecyclerView {
    public NavMenuAwareRecyclerView(Context context) {
        super(context);
    }

    public NavMenuAwareRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavMenuAwareRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        FrameLayoutNavMenuTriggerer.setChildViewScrolled();
    }
}

package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.history.HistoryEntry;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class PageLongPressHandler {
    private final PageViewFragmentInternal fragment;
    private final ViewGroup containerView;
    private PageActivity activity;
    private View anchorView;

    PageLongPressHandler(PageViewFragmentInternal fragment, ViewGroup containerView) {
        this.fragment = fragment;
        this.containerView = containerView;
        this.activity = (PageActivity) fragment.getActivity();
    }

    public void onLongPress(int x, int y, final PageTitle title, final HistoryEntry entry) {
        // create a temporary view at the location of the click event
        anchorView = new View(fragment.getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
        params.leftMargin = x;
        params.topMargin = y;
        anchorView.setLayoutParams(params);
        containerView.addView(anchorView);
        // create a popup menu and anchor it to the temporary view
        PopupMenu popupMenu = new PopupMenu(activity, anchorView);
        popupMenu.inflate(R.menu.menu_page_long_press);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_open_link:
                        activity.displayNewPage(title, entry);
                        return true;
                    case R.id.menu_open_in_new_tab:
                        fragment.openInNewTab(title, entry);
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                if (anchorView != null) {
                    containerView.removeView(anchorView);
                    anchorView = null;
                }
            }
        });
        popupMenu.show();
    }

}

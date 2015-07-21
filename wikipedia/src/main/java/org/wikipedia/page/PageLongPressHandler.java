package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.WikiListView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;

public class PageLongPressHandler {
    private final ViewGroup containerView;
    private final ContextMenuListener contextMenuListener;
    private final View offsetView;
    private int historySource;

    public PageLongPressHandler(@NonNull Window window,
                                @NonNull View offsetView,
                                @NonNull ContextMenuListener listener) {
        this.containerView = (ViewGroup) window.getDecorView();
        this.offsetView = offsetView;
        this.contextMenuListener = listener;
    }

    public PageLongPressHandler(@NonNull Window window,
                                @NonNull final WikiListView listView,
                                @NonNull ContextMenuListener listener, int historySource) {
        this(window, listView, listener);
        this.historySource = historySource;
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = contextMenuListener.getTitleForListPosition(position);
                HistoryEntry entry = new HistoryEntry(title, PageLongPressHandler.this.historySource);
                Utils.hideSoftKeyboard(containerView);
                onLongPress(listView.getLastEventX(), listView.getLastEventY(), title, entry);
                return true;
            }
        });
    }

    public interface ContextMenuListener {
        PageTitle getTitleForListPosition(int position);
        void onOpenLink(PageTitle title, HistoryEntry entry);
        void onOpenInNewTab(PageTitle title, HistoryEntry entry);
    }

    public void onLongPress(int x, int y, final PageTitle title, final HistoryEntry entry) {
        final View menuContainerView = createMenuContainerView(containerView.getContext());
        View menuAnchorView = createMenuAnchorView(menuContainerView, x, y);

        containerView.addView(menuContainerView);
        PopupMenu popupMenu = new PopupMenu(containerView.getContext(), menuAnchorView);
        popupMenu.inflate(R.menu.menu_page_long_press);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_open_link:
                        contextMenuListener.onOpenLink(title, entry);
                        return true;
                    case R.id.menu_open_in_new_tab:
                        contextMenuListener.onOpenInNewTab(title, entry);
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
                if (containerView.indexOfChild(menuContainerView) != -1) {
                    containerView.removeView(menuContainerView);
                }
            }
        });
        popupMenu.show();
    }

    private View createMenuContainerView(Context context) {
        View menuContainerView = new FrameLayout(context);
        ViewGroup.LayoutParams frameParams
                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        menuContainerView.setLayoutParams(frameParams);
        return menuContainerView;
    }

    private View createMenuAnchorView(View menuContainerView, int xPosition, int yPosition) {
        int[] coords = new int[2];
        offsetView.getLocationInWindow(coords);
        menuContainerView.setPadding(xPosition + coords[0], yPosition + coords[1], 0, 0);
        View anchorView = new View(menuContainerView.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
        anchorView.setLayoutParams(params);
        ((ViewGroup) menuContainerView).addView(anchorView);
        return anchorView;
    }
}

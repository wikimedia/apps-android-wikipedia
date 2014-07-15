package org.wikipedia.page;

import android.os.Build;
import android.view.MenuItem;

public class PopupMenu {

    private Object menu;

    public PopupMenu(android.content.Context context, android.view.View anchor) {
        menu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                new android.widget.PopupMenu(context, anchor) :
                new android.support.v7.widget.PopupMenu(context, anchor);

        if (menu instanceof android.support.v7.widget.PopupMenu) {
            ((android.support.v7.widget.PopupMenu) menu).setOnMenuItemClickListener(new android.support.v7.widget.PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (onMenuItemClickListener != null) {
                        return onMenuItemClickListener.onMenuItemClick(menuItem);
                    }
                    return false;
                }
            });
        } else if (menu instanceof android.widget.PopupMenu) {
            ((android.widget.PopupMenu) menu).setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (onMenuItemClickListener != null) {
                        return onMenuItemClickListener.onMenuItemClick(menuItem);
                    }
                    return false;
                }
            });
        }
    }

    public android.view.Menu getMenu() {
        return menu instanceof android.support.v7.widget.PopupMenu ?
                ((android.support.v7.widget.PopupMenu) menu).getMenu() :
                ((android.widget.PopupMenu) menu).getMenu();
    }

    public android.view.MenuInflater getMenuInflater() {
        return menu instanceof android.support.v7.widget.PopupMenu ?
                ((android.support.v7.widget.PopupMenu) menu).getMenuInflater() :
                ((android.widget.PopupMenu) menu).getMenuInflater();
    }

    public void show() {
        if (menu instanceof android.support.v7.widget.PopupMenu) {
            ((android.support.v7.widget.PopupMenu) menu).show();
        } else if (menu instanceof android.widget.PopupMenu) {
            ((android.widget.PopupMenu) menu).show();
        }
    }

    private OnMenuItemClickListener onMenuItemClickListener;

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        onMenuItemClickListener = listener;
    }

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(android.view.MenuItem menuItem);
    }

}

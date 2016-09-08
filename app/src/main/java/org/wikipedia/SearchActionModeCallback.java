package org.wikipedia;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.util.DeviceUtil;

public abstract class SearchActionModeCallback implements ActionMode.Callback {
    private SearchView searchView;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_action_mode_search, menu);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search_view));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                onQueryChange(s);
                return true;
            }
        });
        return true;
    }

    protected abstract void onQueryChange(String s);

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        searchView.requestFocus();
        DeviceUtil.showSoftKeyboard(searchView);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}
package org.wikipedia.history;

import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.wikipedia.R;
import org.wikipedia.util.DeviceUtil;

public abstract class SearchActionModeCallback implements ActionMode.Callback {
    public static final String ACTION_MODE_TAG = "searchActionMode";
    private SearchView searchView;
    private ImageView searchMagIcon;

    public static boolean is(@Nullable ActionMode mode) {
        return mode != null && ACTION_MODE_TAG.equals(mode.getTag());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTag(ACTION_MODE_TAG);
        mode.getMenuInflater().inflate(R.menu.menu_action_mode_search, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search_view).getActionView();
        searchMagIcon = searchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        searchMagIcon.setImageDrawable(null);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getSearchHintString());
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

        searchView.setOnQueryTextFocusChangeListener((view, isFocus) -> {
            if (!isFocus) {
                mode.finish();
            }
        });

        return true;
    }

    protected abstract String getSearchHintString();

    protected abstract void onQueryChange(String s);

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        searchView.requestFocus();
        searchMagIcon.setVisibility(View.GONE);
        DeviceUtil.showSoftKeyboard(searchView);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
        }
    }
}

package org.wikipedia.views;

import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.R;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;

public abstract class MultiSelectActionModeCallback implements ActionMode.Callback {
    private static final String ACTION_MODE_TAG = "multiSelectActionMode";

    public static boolean is(@Nullable ActionMode mode) {
        return mode != null && ACTION_MODE_TAG.equals(mode.getTag());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTag(ACTION_MODE_TAG);
        return true;
    }

    @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete_selected:
                onDeleteSelected();
                return true;
            default:
        }
        return false;
    }

    protected abstract void onDeleteSelected();

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }
}

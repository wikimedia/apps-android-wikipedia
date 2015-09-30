package org.wikipedia.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.R;

/** {@link SearchView} that exposes contextual action bar callbacks. */
public class CabSearchView extends SearchView {
    private static final boolean DEFAULT_CAB_ENABLED = true;

    private boolean mCabEnabled;

    public CabSearchView(Context context) {
        this(context, null);
    }

    public CabSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.searchViewStyle);
    }

    public CabSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        SearchView.SearchAutoComplete searchSrcTextView = (SearchAutoComplete) findViewById(R.id.search_src_text);
        searchSrcTextView.setCustomSelectionActionModeCallback(new Callback());

        initLayoutAttributes(attrs, defStyleAttr);
    }

    public boolean isCabEnabled() {
        return mCabEnabled;
    }

    public void setCabEnabled(boolean enabled) {
        mCabEnabled = enabled;
    }

    private void initLayoutAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray attrsArray = getContext().obtainStyledAttributes(attrs,
                R.styleable.CabSearchView, defStyleAttr, 0);

        setCabEnabled(attrsArray.getBoolean(R.styleable.CabSearchView_cabEnabled,
                DEFAULT_CAB_ENABLED));

        attrsArray.recycle();
    }

    private class Callback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return isCabEnabled();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override public void onDestroyActionMode(ActionMode mode) { }
    }
}
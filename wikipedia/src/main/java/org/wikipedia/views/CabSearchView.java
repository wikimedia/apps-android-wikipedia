package org.wikipedia.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.util.ApiUtil;

/** {@link SearchView} that exposes contextual action bar callbacks. */
public class CabSearchView extends SearchView {
    private static final boolean DEFAULT_CAB_ENABLED = true;
    private static final ActionMode.Callback DEFAULT_CALLBACK;
    static {
        DEFAULT_CALLBACK = ApiUtil.hasHoneyComb() ? new DefaultCallback() : null;
    }

    @NonNull
    private final SearchView.SearchAutoComplete mSearchSrcTextView;

    @NonNull
    private ActionMode.Callback mCallback = DEFAULT_CALLBACK;

    private boolean mCabEnabled;

    public CabSearchView(Context context) {
        this(context, null);
    }

    public CabSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.searchViewStyle);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public CabSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mSearchSrcTextView = (SearchAutoComplete) findViewById(R.id.search_src_text);
        if (ApiUtil.hasHoneyComb()) {
            mSearchSrcTextView.setCustomSelectionActionModeCallback(new CallbackWrapper());
        }

        initLayoutAttributes(attrs, defStyleAttr);
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        return mCallback == DEFAULT_CALLBACK ? null : mCallback;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback callback) {
        mCallback = callback == null ? DEFAULT_CALLBACK : callback;
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

        if (ApiUtil.hasHoneyComb()) {
            setCabEnabled(attrsArray.getBoolean(R.styleable.CabSearchView_cabEnabled,
                    DEFAULT_CAB_ENABLED));
        }

        attrsArray.recycle();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class CallbackWrapper implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return isCabEnabled() && mCallback.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mCallback.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mCallback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mCallback.onDestroyActionMode(mode);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class DefaultCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true; // Show action bar.
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
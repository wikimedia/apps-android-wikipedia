package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentToolbarActivity;

public class LocalCompilationsActivity
        extends SingleFragmentToolbarActivity<LocalCompilationsFragment>
        implements LocalCompilationsFragment.Callback {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, LocalCompilationsActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWordmarkVisible(false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.offline_library_title);
        }
    }

    @Override
    public LocalCompilationsFragment createFragment() {
        return LocalCompilationsFragment.newInstance();
    }

    @Override
    protected void onOfflineCompilationsFound() {
        getFragment().onCompilationsRefreshed();
    }

    @Override
    protected void onOfflineCompilationsError(Throwable t) {
        getFragment().onCompilationsError(t);
    }

    @Override
    public void onRequestUpdateCompilations() {
        searchOfflineCompilationsWithPermission(true);
    }
}

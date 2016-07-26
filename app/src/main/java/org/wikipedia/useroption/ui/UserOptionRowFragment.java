package org.wikipedia.useroption.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.wikipedia.R;
import org.wikipedia.database.CursorAdapterLoaderCallback;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.useroption.sync.UserOptionContentResolver;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.Constants.USER_OPTION_ROW_FRAGMENT_LOADER_ID;

public class UserOptionRowFragment extends Fragment {
    @BindView(R.id.fragment_user_option_list) ListView list;
    private Unbinder unbinder;

    public static UserOptionRowFragment newInstance() {
        return new UserOptionRowFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_user_option_row, container, false);
        unbinder = ButterKnife.bind(this, view);

        CursorAdapter listAdapter = new UserOptionRowCursorAdapter(getContext(), null, true);
        list.setAdapter(listAdapter);

        LoaderCallback callback = new LoaderCallback(getContext(), listAdapter);
        getActivity().getSupportLoaderManager().initLoader(USER_OPTION_ROW_FRAGMENT_LOADER_ID, null,
                callback);

        setHasOptionsMenu(true);

        return view;
    }

    @Override public void onDestroyView() {
        getActivity().getSupportLoaderManager().destroyLoader(USER_OPTION_ROW_FRAGMENT_LOADER_ID);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_user_option, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_option_sync_all:
                UserOptionContentResolver.requestManualSync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static class LoaderCallback extends CursorAdapterLoaderCallback {
        LoaderCallback(@NonNull Context context, @NonNull CursorAdapter adapter) {
            super(context, adapter);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri uri = UserOptionContract.HttpWithOption.URI;
            final String[] projection = null;
            final String selection = null;
            final String[] selectionArgs = null;
            String order = UserOptionContract.Option.ID.qualifiedName() + " desc";
            return new CursorLoader(context(), uri, projection, selection, selectionArgs, order);
        }
    }
}

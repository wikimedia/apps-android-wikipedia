package org.wikipedia.history;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.SearchActionModeCallback;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.database.CursorAdapterLoaderCallback;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.page.PageTitle;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;

public class HistoryFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onLoadPage(PageTitle title, HistoryEntry entry);
        void onClearHistory();
    }

    private ListView historyEntryList;
    private View historyEmptyContainer;
    private TextView historyEmptyTitle;
    private TextView historyEmptyMessage;
    private HistoryEntryAdapter adapter;

    private WikipediaApp app;

    private String currentSearchQuery;
    private LoaderCallback loaderCallback;

    private ActionMode actionMode;
    private SearchActionModeCallback searchActionModeCallback = new HistorySearchCallback();
    private HistoryItemClickListener itemClickListener = new HistoryItemClickListener();
    private HistoryItemLongClickListener itemLongClickListener = new HistoryItemLongClickListener();

    @NonNull public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        app = WikipediaApp.getInstance();
        adapter = new HistoryEntryAdapter(getContext(), null, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        historyEntryList = (ListView) rootView.findViewById(R.id.history_entry_list);
        historyEmptyContainer = rootView.findViewById(R.id.history_empty_container);
        historyEmptyTitle = (TextView) rootView.findViewById(R.id.history_empty_title);
        historyEmptyMessage = (TextView) rootView.findViewById(R.id.history_empty_message);

        historyEntryList.setAdapter(adapter);
        historyEntryList.setOnItemClickListener(itemClickListener);
        historyEntryList.setOnItemLongClickListener(itemLongClickListener);

        loaderCallback = new LoaderCallback(getContext(), adapter);
        getActivity().getSupportLoaderManager().initLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        historyEntryList.setEmptyView(historyEmptyContainer);
    }

    @Override
    public void onDestroyView() {
        historyEntryList.setEmptyView(null);
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_FRAGMENT_LOADER_ID);
        historyEntryList.setOnItemClickListener(null);
        historyEntryList.setOnItemLongClickListener(null);
        historyEntryList.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        if (!isAdded()) {
            return;
        }
        if (!visible && actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
            return true;
        }
        return false;
    }

    private class HistoryEntryAdapter extends CursorAdapter {
        HistoryEntryAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_page_list_entry, viewGroup, false);
            view.setBackgroundResource(R.drawable.selectable_item_background);
            return view;
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.page_list_item_title);
            GoneIfEmptyTextView description = (GoneIfEmptyTextView) view.findViewById(R.id.page_list_item_description);
            HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            description.setText(null);
            view.setTag(entry);
            ViewUtil.loadImageUrlInto((SimpleDraweeView) view.findViewById(R.id.page_list_item_image),
                    PageHistoryContract.PageWithImage.IMAGE_NAME.val(cursor));

            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime, prevTime = "";
            if (cursor.getPosition() != 0) {
                Cursor prevCursor = (Cursor) getItem(cursor.getPosition() - 1);
                HistoryEntry prevEntry = HistoryEntry.DATABASE_TABLE.fromCursor(prevCursor);
                prevTime = getDateString(prevEntry.getTimestamp());
            }
            curTime = getDateString(entry.getTimestamp());
            TextView sectionHeader = (TextView) view.findViewById(R.id.page_list_header_text);
            if (!curTime.equals(prevTime)) {
                sectionHeader.setText(curTime);
                sectionHeader.setVisibility(View.VISIBLE);
            } else {
                sectionHeader.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isHistoryAvailable = historyEntryList.getCount() > 0;
        menu.findItem(R.id.menu_clear_all_history)
                .setVisible(isHistoryAvailable)
                .setEnabled(isHistoryAvailable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_all_history:
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dialog_title_clear_history)
                        .setMessage(R.string.dialog_message_clear_history)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear history!
                                new DeleteAllHistoryTask(app).execute();
                                onClearHistoryClick();
                        }
                        })
                        .setNegativeButton(R.string.no, null).create().show();
                return true;
            case R.id.menu_search_history:
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) getActivity())
                            .startSupportActionMode(searchActionModeCallback);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class HistoryItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (actionMode == null) {
                HistoryEntry oldEntry = (HistoryEntry) view.getTag();
                HistoryEntry newEntry = new HistoryEntry(oldEntry.getTitle(), HistoryEntry.SOURCE_HISTORY);
                onPageClick(oldEntry.getTitle(), newEntry);
            } else {
                actionMode.invalidate();
            }
        }
    }

    private class HistoryItemLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (actionMode != null) {
                return false;
            }
            historyEntryList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(new ActionMode.Callback() {
                private final String actionModeTag = "actionModeHistory";
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.menu_history_context, menu);
                    setActionModeIntTitle(historyEntryList.getCheckedItemCount() + 1, mode);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    mode.setTag(actionModeTag);
                    int count = historyEntryList.getCheckedItemCount();
                    if (actionMode != null) {
                        if (count == 0) {
                            mode.finish();
                        } else {
                            setActionModeIntTitle(count, mode);
                        }
                    }
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.menu_delete_selected_history) {
                        SparseBooleanArray checkedItems = historyEntryList.getCheckedItemPositions();
                        for (int i = 0; i < checkedItems.size(); i++) {
                            if (checkedItems.valueAt(i)) {
                                app.getDatabaseClient(HistoryEntry.class).delete(
                                        HistoryEntry.DATABASE_TABLE.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i))),
                                        PageHistoryContract.PageWithImage.SELECTION);
                            }
                        }
                        mode.finish();
                        return true;
                    } else {
                        throw new RuntimeException("Unknown context menu item clicked");
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    historyEntryList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                    actionMode = null;
                    // Clear all selections
                    historyEntryList.clearChoices();
                    historyEntryList.requestLayout(); // Required to immediately redraw unchecked states
                }
            });
            historyEntryList.setItemChecked(position, true);
            return true;
        }
    }

    private class LoaderCallback extends CursorAdapterLoaderCallback {
        LoaderCallback(@NonNull Context context, @NonNull CursorAdapter adapter) {
            super(context, adapter);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String titleCol = PageHistoryContract.PageWithImage.TITLE.qualifiedName();
            String selection = null;
            String[] selectionArgs = null;
            historyEmptyContainer.setVisibility(View.GONE);
            String searchStr = currentSearchQuery;
            if (!TextUtils.isEmpty(searchStr)) {
                searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                selection = "UPPER(" + titleCol + ") LIKE UPPER(?) ESCAPE '\\'";
                selectionArgs = new String[]{"%" + searchStr + "%"};
            }

            Uri uri = PageHistoryContract.PageWithImage.URI;
            final String[] projection = null;
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return new CursorLoader(context(), uri, projection, selection, selectionArgs, order);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            super.onLoadFinished(cursorLoader, cursor);
            if (!isAdded()) {
                return;
            }
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void setActionModeIntTitle(int count, ActionMode mode) {
        mode.setTitle(NumberFormat.getInstance().format(count));
    }

    private void onPageClick(PageTitle title, HistoryEntry entry) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(title, entry);
        }
    }

    private void onClearHistoryClick() {
        Callback callback = callback();
        if (callback != null) {
            callback.onClearHistory();
        }
    }

    private void restartLoader() {
        getActivity().getSupportLoaderManager().restartLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
    }

    private class HistorySearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s;
            restartLoader();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            historyEmptyTitle.setText(getString(R.string.history_search_empty_message));
            historyEmptyMessage.setVisibility(View.GONE);
            return super.onPrepareActionMode(mode, menu);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                currentSearchQuery = "";
                restartLoader();
            }
            historyEmptyTitle.setText(R.string.history_empty_title);
            historyEmptyMessage.setVisibility(View.VISIBLE);
            actionMode = null;
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}

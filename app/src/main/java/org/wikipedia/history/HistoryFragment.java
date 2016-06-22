package org.wikipedia.history;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.CursorAdapterLoaderCallback;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.MainActivity;
import org.wikipedia.views.ViewUtil;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class HistoryFragment extends Fragment implements BackPressedHandler {
    private ListView historyEntryList;
    private View historyEmptyContainer;
    private TextView historyEmptyTitle;
    private TextView historyEmptyMessage;
    private HistoryEntryAdapter adapter;
    private EditText entryFilter;

    private WikipediaApp app;

    private LoaderCallback callback;

    private ActionMode actionMode;
    private HistorySearchTextWatcher textWatcher = new HistorySearchTextWatcher();
    private HistoryItemClickListener itemClickListener = new HistoryItemClickListener();
    private HistoryItemLongClickListener itemLongClickListener = new HistoryItemLongClickListener();
    private boolean firstRun = true;

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
        rootView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);
        historyEntryList = (ListView) rootView.findViewById(R.id.history_entry_list);
        historyEmptyContainer = rootView.findViewById(R.id.history_empty_container);
        historyEmptyTitle = (TextView) rootView.findViewById(R.id.history_empty_title);
        historyEmptyMessage = (TextView) rootView.findViewById(R.id.history_empty_message);
        entryFilter = (EditText) rootView.findViewById(R.id.history_search_list);

        entryFilter.addTextChangedListener(textWatcher);
        historyEntryList.setAdapter(adapter);
        historyEntryList.setOnItemClickListener(itemClickListener);
        historyEntryList.setOnItemLongClickListener(itemLongClickListener);

        callback = new LoaderCallback(getContext(), adapter);
        getActivity().getSupportLoaderManager().initLoader(HISTORY_FRAGMENT_LOADER_ID, null, callback);

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
        entryFilter.removeTextChangedListener(textWatcher);
        historyEntryList.setOnItemClickListener(null);
        historyEntryList.setOnItemLongClickListener(null);
        historyEntryList.setAdapter(null);
        super.onDestroyView();
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
            HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
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
        if (!isMenuToBeSetUp()) {
            return;
        }
        menu.clear();
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isMenuToBeSetUp()) {
            return;
        }
        boolean isHistoryAvailable = historyEntryList.getCount() > 0;
        menu.findItem(R.id.menu_clear_all_history)
                .setVisible(isHistoryAvailable)
                .setEnabled(isHistoryAvailable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_all_history:
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_title_clear_history)
                        .setMessage(R.string.dialog_message_clear_history)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear history!
                                new DeleteAllHistoryTask(app).execute();
                                entryFilter.setVisibility(View.GONE);
                                ((MainActivity) getActivity()).resetAfterClearHistory();
                            }
                        })
                        .setNegativeButton(R.string.no, null).create().show();
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
                ((MainActivity) getActivity()).loadPage(oldEntry.getTitle(), newEntry);
            } else {
                actionMode.invalidate();
            }
        }
    }

    private boolean isMenuToBeSetUp() {
        return isAdded() && !((MainActivity)getActivity()).isSearching();
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
                        if (checkedItems.size() == historyEntryList.getAdapter().getCount()) {
                            entryFilter.setVisibility(View.GONE);
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

    private class HistorySearchTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // Do nothing
        }

        @Override
        public void afterTextChanged(Editable editable) {
            getActivity().getSupportLoaderManager().restartLoader(HISTORY_FRAGMENT_LOADER_ID, null, callback);
            if (editable.length() == 0) {
                historyEmptyTitle.setText(R.string.history_empty_title);
                historyEmptyMessage.setVisibility(View.VISIBLE);
            } else {
                historyEmptyTitle.setText(getString(R.string.history_search_empty_message, editable.toString()));
                historyEmptyMessage.setVisibility(View.GONE);
            }
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
            String searchStr = entryFilter.getText().toString();
            if (!searchStr.isEmpty()) {
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

            // Hide search bar if history is empty, but not when a search turns up no results
            if (firstRun && cursor.getCount() == 0) {
                entryFilter.setVisibility(View.GONE);
            }
            firstRun = false;

            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void setActionModeIntTitle(int count, ActionMode mode) {
        mode.setTitle(NumberFormat.getInstance().format(count));
    }
}

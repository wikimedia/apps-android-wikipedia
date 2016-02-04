package org.wikipedia.history;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.views.ViewUtil;

import java.text.DateFormat;
import java.util.Date;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, BackPressedHandler {
    // make sure this number is unique among other fragments that use a loader
    private static final int LOADER_ID = 100;

    private ListView historyEntryList;
    private View historyEmptyContainer;
    private TextView historyEmptyTitle;
    private TextView historyEmptyMessage;
    private HistoryEntryAdapter adapter;
    private EditText entryFilter;

    private WikipediaApp app;

    private ActionMode actionMode;

    private boolean firstRun = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        app = WikipediaApp.getInstance();
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

        app.adjustDrawableToTheme(((ImageView) rootView.findViewById(R.id.history_empty_image)).getDrawable());
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new HistoryEntryAdapter(getActivity(), null, true);
        historyEntryList.setAdapter(adapter);
        historyEntryList.setEmptyView(historyEmptyContainer);

        entryFilter.addTextChangedListener(
                new TextWatcher() {
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
                        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, HistoryFragment.this);
                        if (editable.length() == 0) {
                            historyEmptyTitle.setText(R.string.history_empty_title);
                            historyEmptyMessage.setVisibility(View.VISIBLE);
                        } else {
                            historyEmptyTitle.setText(getString(R.string.history_search_empty_message, editable.toString()));
                            historyEmptyMessage.setVisibility(View.GONE);
                        }
                    }
                });

        historyEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode == null) {
                    HistoryEntry oldEntry = (HistoryEntry) view.getTag();
                    HistoryEntry newEntry = new HistoryEntry(oldEntry.getTitle(), HistoryEntry.SOURCE_HISTORY);
                    ((PageActivity) getActivity()).loadPage(oldEntry.getTitle(), newEntry);
                }
            }
        });

        historyEntryList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
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
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        mode.setTag(actionModeTag);
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
                                                    HistoryEntryDatabaseTable.SELECTION_KEYS);
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
        });

        getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onDestroyView() {
        getActivity().getSupportLoaderManager().destroyLoader(LOADER_ID);
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (!isAdded()) {
            return null;
        }
        String selection = null;
        String[] selectionArgs = null;
        historyEmptyContainer.setVisibility(View.GONE);
        String searchStr = entryFilter.getText().toString();
        if (searchStr.length() != 0) {
            // FIXME: Find ways to not have to hard code column names
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            selection =  "UPPER(history.title) LIKE UPPER(?) ESCAPE '\\'";
            selectionArgs = new String[]{"%" + searchStr + "%"};
        }
        return new CursorLoader(
                getActivity(),
                Uri.parse(HistoryEntry.DATABASE_TABLE.getBaseContentURI().toString() + "/" + PageImage.DATABASE_TABLE

                        .getTableName()),
                null,
                selection,
                selectionArgs,
                "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded() || cursor == null) {
            return;
        }

        // Hide search bar if history is empty, but not when a search turns up no results
        if (firstRun && cursor.getCount() == 0) {
            entryFilter.setVisibility(View.GONE);
        }
        firstRun = false;

        adapter.swapCursor(cursor);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        adapter.changeCursor(null);
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
                    cursor.getString(HistoryEntryContentProvider.COL_INDEX_IMAGE));

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
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        menu.findItem(R.id.menu_clear_all_history)
            .setVisible(historyEntryList.getCount() > 0)
            .setEnabled(historyEntryList.getCount() > 0);
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
                                ((PageActivity) getActivity()).resetAfterClearHistory();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Uh, do nothing?
                            }
                        }).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

package org.wikipedia.history;

import android.app.AlertDialog;
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
import android.support.v7.app.ActionBarActivity;
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

import com.squareup.picasso.Picasso;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.pageimages.PageImage;

import java.text.DateFormat;
import java.util.Date;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);

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
                    ((PageActivity)getActivity()).displayNewPage(oldEntry.getTitle(), newEntry);
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
                actionMode = ((ActionBarActivity)getActivity()).startSupportActionMode(new ActionMode.Callback() {
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
                                    app.getPersister(HistoryEntry.class).delete(
                                        HistoryEntry.PERSISTENCE_HELPER.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)))
                                    );
                                }
                            }
                            actionMode.finish();
                            return true;
                        } else {
                            // This can't happen
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
                Uri.parse(HistoryEntry.PERSISTENCE_HELPER.getBaseContentURI().toString() + "/" + PageImage.PERSISTENCE_HELPER

                        .getTableName()),
                null,
                selection,
                selectionArgs,
                "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        if (!isAdded()) {
            return;
        }
        adapter.swapCursor(cursorLoader);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoaderLoader) {
        adapter.changeCursor(null);
    }

    private class HistoryEntryAdapter extends CursorAdapter {
        public HistoryEntryAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getActivity().getLayoutInflater().inflate(R.layout.item_history_entry, viewGroup, false);
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }

        private int getImageForSource(int source) {
            switch (source) {
                case HistoryEntry.SOURCE_INTERNAL_LINK:
                    return R.drawable.link;
                case HistoryEntry.SOURCE_EXTERNAL_LINK:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_HISTORY:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_SEARCH:
                    return R.drawable.hist_search;
                case HistoryEntry.SOURCE_SAVED_PAGE:
                    return R.drawable.external;
                case HistoryEntry.SOURCE_LANGUAGE_LINK:
                    return R.drawable.link;
                case HistoryEntry.SOURCE_RANDOM:
                    return R.drawable.random;
                case HistoryEntry.SOURCE_MAIN_PAGE:
                    return R.drawable.link;
                case HistoryEntry.SOURCE_NEARBY:
                    return R.drawable.ic_place;
                default:
                    throw new RuntimeException("Unknown source id encountered");
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.history_title);
            ImageView source = (ImageView) view.findViewById(R.id.history_source);
            ImageView thumbnail = (ImageView) view.findViewById(R.id.history_thumbnail);
            HistoryEntry entry = HistoryEntry.PERSISTENCE_HELPER.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            source.setImageResource(getImageForSource(entry.getSource()));
            view.setTag(entry);

            if (app.showImages()) {
                Picasso.with(getActivity())
                       .load(cursor.getString(HistoryEntryContentProvider.COL_INDEX_IMAGE))
                       .placeholder(R.drawable.ic_pageimage_placeholder)
                       .error(R.drawable.ic_pageimage_placeholder)
                       .into(thumbnail);
            } else {
                Picasso.with(getActivity())
                       .load(R.drawable.ic_pageimage_placeholder)
                       .into(thumbnail);
            }

            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime, prevTime = "";
            if (cursor.getPosition() != 0) {
                Cursor prevCursor = (Cursor) getItem(cursor.getPosition() - 1);
                HistoryEntry prevEntry = HistoryEntry.PERSISTENCE_HELPER.fromCursor(prevCursor);
                prevTime = getDateString(prevEntry.getTimestamp());
            }
            curTime = getDateString(entry.getTimestamp());
            TextView sectionHeader = (TextView) view.findViewById(R.id.history_section_header_text);
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
        app.adjustDrawableToTheme(menu.findItem(R.id.menu_clear_all_history).getIcon());
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        menu.findItem(R.id.menu_clear_all_history).setEnabled(historyEntryList.getCount() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return false;
            case R.id.menu_clear_all_history:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.dialog_title_clear_history);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear history!
                        app.getPersister(HistoryEntry.class).deleteAll();
                    }
                });

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Uh, do nothing?
                    }
                });
                builder.create().show();
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked!");
        }
    }
}

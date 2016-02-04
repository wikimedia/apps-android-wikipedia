package org.wikipedia.savedpages;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class SavedPagesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, BackPressedHandler {
    // make sure this number is unique among other fragments that use a loader
    private static final int LOADER_ID = 101;

    private ListView savedPagesList;
    private View savedPagesEmptyContainer;
    private TextView savedPagesEmptyTitle;
    private TextView savedPagesEmptyMessage;
    private SavedPagesAdapter adapter;
    private RefreshPagesHandler refreshHandler;
    private EditText entryFilter;
    private ImageView savedPagesEmptyImage;

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
        View rootView = inflater.inflate(R.layout.fragment_saved_pages, container, false);
        rootView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);

        savedPagesList = (ListView) rootView.findViewById(R.id.saved_pages_list);
        savedPagesEmptyContainer = rootView.findViewById(R.id.saved_pages_empty_container);
        savedPagesEmptyTitle = (TextView) rootView.findViewById(R.id.saved_pages_empty_title);
        savedPagesEmptyMessage = (TextView) rootView.findViewById(R.id.saved_pages_empty_message);
        entryFilter = (EditText) rootView.findViewById(R.id.saved_pages_search_list);
        savedPagesEmptyImage = (ImageView) rootView.findViewById(R.id.saved_pages_empty_image);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new SavedPagesAdapter(getActivity(), null, true);
        savedPagesList.setAdapter(adapter);
        savedPagesList.setEmptyView(savedPagesEmptyContainer);

        savedPagesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode != null) {
                    return false;
                }
                savedPagesList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(new ActionMode.Callback() {
                    private final String actionModeTag = "actionModeSavedPages";
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        mode.getMenuInflater().inflate(R.menu.menu_saved_pages_context, menu);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        mode.setTag(actionModeTag);
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_refresh_selected_saved_pages:
                                refreshSelected();
                                actionMode.finish();
                                return true;
                            case R.id.menu_delete_selected_saved_pages:
                                deleteSelected();
                                actionMode.finish();
                                return true;
                            default:
                                throw new RuntimeException("Unknown context menu item clicked");
                        }
                    }

                    private void deleteSelected() {
                        SparseBooleanArray checkedItems = savedPagesList.getCheckedItemPositions();
                        for (int i = 0; i < checkedItems.size(); i++) {
                            if (checkedItems.valueAt(i)) {
                                final SavedPage page = SavedPage.DATABASE_TABLE.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)));
                                new DeleteSavedPageTask(getActivity(), page) {
                                    @Override
                                    public void onFinish(Boolean result) {
                                        if (!isAdded()) {
                                            return;
                                        }
                                        FeedbackUtil.showMessage(getActivity(),
                                                R.string.snackbar_saved_page_deleted);
                                    }
                                }.execute();
                            }
                        }
                        if (checkedItems.size() == savedPagesList.getAdapter().getCount()) {
                            entryFilter.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        savedPagesList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                        actionMode = null;
                        // Clear all selections
                        savedPagesList.clearChoices();
                        savedPagesList.requestLayout(); // Required to immediately redraw unchecked states

                    }
                });
                savedPagesList.setItemChecked(position, true);
                return true;
            }
        });

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
                        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, SavedPagesFragment.this);
                        if (editable.length() == 0) {
                            savedPagesEmptyTitle.setText(R.string.saved_pages_empty_title);
                            savedPagesEmptyImage.setVisibility(View.VISIBLE);
                            savedPagesEmptyMessage.setVisibility(View.VISIBLE);
                        } else {
                            savedPagesEmptyTitle.setText(getString(R.string.saved_pages_search_empty_message, editable.toString()));
                            savedPagesEmptyImage.setVisibility(View.GONE);
                            savedPagesEmptyMessage.setVisibility(View.GONE);
                        }
                    }
                });

        savedPagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // We shouldn't do anything if the user is multi-selecting things
                if (actionMode == null) {
                    SavedPage savedPage = (SavedPage) view.getTag();
                    HistoryEntry newEntry = new HistoryEntry(savedPage.getTitle(), HistoryEntry.SOURCE_SAVED_PAGE);
                    ((PageActivity)getActivity()).loadPage(savedPage.getTitle(), newEntry);
                }
            }
        });

        app.adjustDrawableToTheme(savedPagesEmptyImage.getDrawable());

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
        savedPagesEmptyContainer.setVisibility(View.GONE);
        String searchStr = entryFilter.getText().toString();
        if (searchStr.length() != 0) {
            // FIXME: Find ways to not have to hard code column names
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            selection =  "UPPER(savedpages.title) LIKE UPPER(?) ESCAPE '\\'";
            selectionArgs = new String[]{"%" + searchStr + "%"};
        }
        return new CursorLoader(
                getActivity(),
                Uri.parse(SavedPage.DATABASE_TABLE.getBaseContentURI().toString() + "/" + PageImage.DATABASE_TABLE

                        .getTableName()),
                null,
                selection,
                selectionArgs,
                "savedpages.title ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded() || cursor == null) {
            return;
        }

        // Hide search bar if no saved pages exist, but not when a search turns up no results
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

    private class SavedPagesAdapter extends CursorAdapter {
        SavedPagesAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_page_list_entry, viewGroup, false);
            view.setBackgroundResource(R.drawable.selectable_item_background);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.page_list_item_title);
            SavedPage entry = SavedPage.DATABASE_TABLE.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            view.setTag(entry);
            ViewUtil.loadImageUrlInto((SimpleDraweeView) view.findViewById(R.id.page_list_item_image),
                    cursor.getString(SavedPageContentProvider.COL_INDEX_IMAGE));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        inflater.inflate(R.menu.menu_saved_pages, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }

        // Only enable and show the buttons in the options menu if there are saved pages in the list
        boolean enabled = savedPagesList.getCount() > 0;
        for (int id : Arrays.asList(R.id.menu_clear_all_saved_pages, R.id.menu_refresh_all_saved_pages)) {
            menu.findItem(id).setEnabled(enabled).setVisible(enabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_all_saved_pages:
                promptToRefreshAll();
                return true;
            case R.id.menu_clear_all_saved_pages:
                promptToDeleteAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void promptToRefreshAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_prompt_refresh_all_saved_pages);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                refreshAll();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    private void promptToDeleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_title_clear_saved_pages);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new DeleteAllSavedPagesTask(getActivity()) {
                    @Override
                    public void onFinish(Void v) {
                        if (!isAdded()) {
                            return;
                        }
                        entryFilter.setVisibility(View.GONE);
                        FeedbackUtil.showMessage(getActivity(),
                                R.string.snackbar_saved_page_deleted);
                    }
                }.execute();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    private void refreshSelected() {
        SparseBooleanArray checkedItems = savedPagesList.getCheckedItemPositions();
        List<SavedPage> savedPages = new ArrayList<>();
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i)) {
                SavedPage page = SavedPage.DATABASE_TABLE.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)));
                savedPages.add(page);
            }
        }
        refreshHandler = new RefreshPagesHandler(getActivity(), savedPages);
        refreshHandler.refresh();
    }

    private void refreshAll() {
        List<SavedPage> savedPages = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            SavedPage page = SavedPage.DATABASE_TABLE.fromCursor((Cursor) adapter.getItem(i));
            savedPages.add(page);
        }
        refreshHandler = new RefreshPagesHandler(getActivity(), savedPages);
        refreshHandler.refresh();
    }

    @Override
    public void onStop() {
        if (refreshHandler != null) {
            refreshHandler.onStop();
        }
        super.onStop();
    }
}

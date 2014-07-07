package org.wikipedia.savedpages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import org.wikipedia.R;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.pageimages.PageImage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SavedPagesActivity extends ThemedActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int ACTIVITY_RESULT_SAVEDPAGE_SELECT = 1;

    private ListView savedPagesList;
    private View savedPagesEmpty;
    private SavedPagesAdapter adapter;
    private RefreshPagesHandler refreshHandler;

    private WikipediaApp app;

    private ActionMode actionMode;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_saved_pages);
        savedPagesList = (ListView) findViewById(R.id.saved_pages_list);
        savedPagesEmpty = findViewById(R.id.saved_pages_empty_container);

        adapter = new SavedPagesAdapter(this, null, true);
        savedPagesList.setAdapter(adapter);
        savedPagesList.setEmptyView(savedPagesEmpty);

        savedPagesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode != null) {
                    return false;
                }
                savedPagesList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                actionMode = startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        mode.getMenuInflater().inflate(R.menu.menu_saved_pages_context, menu);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
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
                                // This can't happen
                                throw new RuntimeException("Unknown context menu item clicked");
                        }
                    }


                    private void deleteSelected() {
                        SparseBooleanArray checkedItems = savedPagesList.getCheckedItemPositions();
                        for (int i = 0; i < checkedItems.size(); i++) {
                            if (checkedItems.valueAt(i)) {
                                final SavedPage page = SavedPage.PERSISTANCE_HELPER.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)));
                                new DeleteSavedPageTask(SavedPagesActivity.this, page) {
                                    @Override
                                    public void onFinish(Boolean result) {
                                        Toast.makeText(SavedPagesActivity.this, R.string.toast_saved_page_deleted, Toast.LENGTH_SHORT).show();
                                    }
                                }.execute();
                            }
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

        savedPagesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // We shouldn't do anything if the user is multiselecting things
                if (actionMode == null) {
                    SavedPage savedPage = (SavedPage) view.getTag();
                    HistoryEntry newEntry = new HistoryEntry(savedPage.getTitle(), HistoryEntry.SOURCE_SAVED_PAGE);

                    Intent intent = new Intent();
                    intent.setClass(SavedPagesActivity.this, PageActivity.class);
                    intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                    intent.putExtra(PageActivity.EXTRA_PAGETITLE, savedPage.getTitle());
                    intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, newEntry);
                    setResult(ACTIVITY_RESULT_SAVEDPAGE_SELECT, intent);
                    finish();
                }
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
        app.adjustDrawableToTheme( ((ImageView)findViewById(R.id.saved_pages_empty_image)).getDrawable() );
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                this,
                Uri.parse(SavedPage.PERSISTANCE_HELPER.getBaseContentURI().toString() + "/" + PageImage.PERSISTANCE_HELPER.getTableName()),
                null,
                null,
                null,
                "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        adapter.swapCursor(cursorLoader);
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoaderLoader) {
        adapter.changeCursor(null);
    }

    private class SavedPagesAdapter extends CursorAdapter {
        public SavedPagesAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(R.layout.item_saved_page_entry, viewGroup, false);
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.saved_page_title);
            ImageView thumbnail = (ImageView) view.findViewById(R.id.saved_page_thumbnail);
            SavedPage entry = SavedPage.PERSISTANCE_HELPER.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            view.setTag(entry);

            Picasso.with(SavedPagesActivity.this)
                    .load(cursor.getString(4))
                    .placeholder(R.drawable.ic_pageimage_placeholder)
                    .error(R.drawable.ic_pageimage_placeholder)
                    .into(thumbnail);

            // Check the previous item, see if the times differe enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime, prevTime = "";
            if (cursor.getPosition() != 0) {
                Cursor prevCursor = (Cursor) getItem(cursor.getPosition() - 1);
                SavedPage prevEntry = SavedPage.PERSISTANCE_HELPER.fromCursor(prevCursor);
                prevTime = getDateString(prevEntry.getTimestamp());
            }
            curTime = getDateString(entry.getTimestamp());
            TextView sectionHeader = (TextView) view.findViewById(R.id.saved_page_section_header_text);
            if (!curTime.equals(prevTime)) {
                sectionHeader.setText(curTime);
                sectionHeader.setVisibility(View.VISIBLE);
            } else {
                sectionHeader.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saved_pages, menu);
        app.adjustDrawableToTheme(menu.findItem(R.id.menu_refresh_all_saved_pages).getIcon());
        app.adjustDrawableToTheme(menu.findItem(R.id.menu_clear_all_saved_pages).getIcon());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_clear_all_saved_pages).setEnabled(savedPagesList.getCount() > 0);
        menu.findItem(R.id.menu_refresh_all_saved_pages).setEnabled(savedPagesList.getCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_refresh_all_saved_pages:
                promptToRefreshAll();
                return true;
            case R.id.menu_clear_all_saved_pages:
                promptToDeleteAll();
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked!");
        }
    }

    private void promptToRefreshAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_title_clear_saved_pages);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new DeleteAllSavedPagesTask(SavedPagesActivity.this) {
                    @Override
                    public void onFinish(Void v) {
                        Toast.makeText(SavedPagesActivity.this, R.string.toast_saved_page_deleted, Toast.LENGTH_SHORT).show();
                    }
                }.execute();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.create().show();
    }

    private void refreshSelected() {
        SparseBooleanArray checkedItems = savedPagesList.getCheckedItemPositions();
        ArrayList<SavedPage> savedPages = new ArrayList<SavedPage>();
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i)) {
                SavedPage page = SavedPage.PERSISTANCE_HELPER.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)));
                savedPages.add(page);
            }
        }
        refreshHandler = new RefreshPagesHandler(SavedPagesActivity.this, savedPages);
        refreshHandler.refresh();
    }

    private void refreshAll() {
        ArrayList<SavedPage> savedPages = new ArrayList<SavedPage>();
        for (int i = 0; i < adapter.getCount(); i++) {
            SavedPage page = SavedPage.PERSISTANCE_HELPER.fromCursor((Cursor) adapter.getItem(i));
            savedPages.add(page);
        }
        refreshHandler = new RefreshPagesHandler(SavedPagesActivity.this, savedPages);
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

package org.wikipedia.bookmarks;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.*;
import android.support.v7.view.ActionMode;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.squareup.picasso.*;
import org.wikipedia.*;
import org.wikipedia.history.*;
import org.wikipedia.page.*;
import org.wikipedia.pageimages.*;

import java.text.*;
import java.util.*;

public class BookmarksActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private ListView savedPagesList;
    private View savedPagesEmpty;
    private BookmarksAdapter adapter;

    private WikipediaApp app;

    private ActionMode actionMode;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_saved_pages);
        savedPagesList = (ListView) findViewById(R.id.saved_pages_list);
        savedPagesEmpty = findViewById(R.id.saved_pages_empty_message);

        adapter = new BookmarksAdapter(this, null, true);
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
                            case R.id.menu_delete_selected_saved_pages:
                                SparseBooleanArray checkedItems = savedPagesList.getCheckedItemPositions();
                                for (int i = 0; i < checkedItems.size(); i++) {
                                    if (checkedItems.valueAt(i)) {
                                        final Bookmark page = Bookmark.PERSISTANCE_HELPER.fromCursor((Cursor) adapter.getItem(checkedItems.keyAt(i)));
                                        new DeleteBookmarkTask(BookmarksActivity.this, page) {
                                            @Override
                                            public void onFinish(Boolean result) {
                                                Toast.makeText(BookmarksActivity.this, R.string.toast_saved_page_deleted, Toast.LENGTH_SHORT).show();
                                            }
                                        }.execute();
                                    }
                                }
                                actionMode.finish();
                                return true;
                            default:
                                // This can't happen
                                throw new RuntimeException("Unknown context menu item clicked");
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
                    Bookmark bookmark = (Bookmark) view.getTag();
                    HistoryEntry newEntry = new HistoryEntry(bookmark.getTitle(), HistoryEntry.SOURCE_SAVED_PAGE);

                    Intent intent = new Intent();
                    intent.setClass(BookmarksActivity.this, PageActivity.class);
                    intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                    intent.putExtra(PageActivity.EXTRA_PAGETITLE, bookmark.getTitle());
                    intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, newEntry);
                    startActivity(intent);
                }
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                this,
                Uri.parse(Bookmark.PERSISTANCE_HELPER.getBaseContentURI().toString() + "/" + PageImage.PERSISTANCE_HELPER.getTableName()),
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

    private class BookmarksAdapter extends CursorAdapter {
        public BookmarksAdapter(Context context, Cursor c, boolean autoRequery) {
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
            Bookmark entry = Bookmark.PERSISTANCE_HELPER.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            view.setTag(entry);

            Picasso.with(BookmarksActivity.this)
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
                Bookmark prevEntry = Bookmark.PERSISTANCE_HELPER.fromCursor(prevCursor);
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_saved_pages, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_clear_all_saved_pages).setEnabled(savedPagesList.getCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_clear_all_saved_pages:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dialog_title_clear_saved_pages)
                        .setMessage(R.string.dialog_message_clear_saved_pages);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear Saved Pages!
                        app.getPersister(Bookmark.class).deleteAll();
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
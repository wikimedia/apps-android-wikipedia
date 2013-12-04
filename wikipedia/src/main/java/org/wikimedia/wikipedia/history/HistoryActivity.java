package org.wikimedia.wikipedia.history;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.*;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.wikimedia.wikipedia.PageActivity;
import org.wikimedia.wikipedia.R;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;

import java.text.DateFormat;
import java.util.Date;

public class HistoryActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private ListView historyEntryList;
    private HistoryEntryAdapter adapter;

    private WikipediaApp app;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_history);
        historyEntryList = (ListView) findViewById(R.id.history_entry_list);

        adapter = new HistoryEntryAdapter(this, null, true);
        historyEntryList.setAdapter(adapter);

        historyEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HistoryEntry oldEntry = (HistoryEntry)view.getTag();
                HistoryEntry newEntry = new HistoryEntry(oldEntry.getTitle(), HistoryEntry.SOURCE_HISTORY);

                Intent intent = new Intent();
                intent.setClass(HistoryActivity.this, PageActivity.class);
                intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                intent.putExtra(PageActivity.EXTRA_PAGETITLE, oldEntry.getTitle());
                intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, newEntry);
                startActivity(intent);
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                throw new RuntimeException("Unknown menu item selected");
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                this,
                HistoryEntry.persistanceHelper.getBaseContentURI(),
                new String[] {"*"},
                null,
                null,
                "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        adapter.swapCursor(cursorLoader);
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
            return getLayoutInflater().inflate(R.layout.item_history_entry, viewGroup, false);
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
                    return R.drawable.search;
                default:
                    throw new RuntimeException("Unknown source id encountered");
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.history_title);
            ImageView source = (ImageView) view.findViewById(R.id.history_source);
            HistoryEntry entry = HistoryEntry.persistanceHelper.fromCursor(cursor);
            title.setText(entry.getTitle().getDisplayText());
            source.setImageResource(getImageForSource(entry.getSource()));
            view.setTag(entry);

            // Check the previous item, see if the times differe enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime, prevTime = "";
            if (cursor.getPosition() != 0) {
                Cursor prevCursor = (Cursor) getItem(cursor.getPosition() - 1);
                HistoryEntry prevEntry = HistoryEntry.persistanceHelper.fromCursor(prevCursor);
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
}
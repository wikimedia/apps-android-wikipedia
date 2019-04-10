package org.wikipedia.readinglist;

import android.content.Context;
import android.content.Intent;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.readinglist.database.ReadingList;

import androidx.annotation.NonNull;

public class ReadingListActivity extends SingleFragmentActivity<ReadingListFragment> {
    protected static final String EXTRA_READING_LIST_ID = "readingListId";

    public static Intent newIntent(@NonNull Context context, @NonNull ReadingList list) {
        return new Intent(context, ReadingListActivity.class)
                .putExtra(EXTRA_READING_LIST_ID, list.id());
    }

    @Override
    public ReadingListFragment createFragment() {
        return ReadingListFragment.newInstance(getIntent().getLongExtra(EXTRA_READING_LIST_ID, 0));
    }
}

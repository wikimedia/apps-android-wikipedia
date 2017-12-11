package org.wikipedia.readinglist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.readinglist.database.ReadingList;

public class ReadingListActivity extends SingleFragmentActivity<ReadingListFragment> {
    protected static final String EXTRA_READING_LIST_ID = "readingListId";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public static Intent newIntent(@NonNull Context context, @NonNull ReadingList list) {
        return new Intent(context, ReadingListActivity.class)
                .putExtra(EXTRA_READING_LIST_ID, list.id());
    }

    @Override
    public ReadingListFragment createFragment() {
        return ReadingListFragment.newInstance(getIntent().getLongExtra(EXTRA_READING_LIST_ID, 0));
    }
}

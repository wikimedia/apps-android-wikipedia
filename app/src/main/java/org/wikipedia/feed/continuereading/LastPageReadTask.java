package org.wikipedia.feed.continuereading;

import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageBackStackItem;

import java.util.List;

public class LastPageReadTask extends SaneAsyncTask<HistoryEntry> {
    private final int age;

    public LastPageReadTask(int age) {
        this.age = age;
    }

    @Nullable @Override public HistoryEntry performTask() throws Throwable {
        if (age < WikipediaApp.getInstance().getTabList().size()) {
            List<PageBackStackItem> items =  WikipediaApp.getInstance().getTabList().get(WikipediaApp.getInstance().getTabList().size() - age - 1).getBackStack();
            if (!items.isEmpty()) {
                return items.get(items.size() - 1).getHistoryEntry();
            }
        }
        return null;
    }
}

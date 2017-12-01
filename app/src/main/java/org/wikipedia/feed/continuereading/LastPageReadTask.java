package org.wikipedia.feed.continuereading;

import android.support.annotation.Nullable;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.settings.Prefs;

import java.util.List;

public class LastPageReadTask extends SaneAsyncTask<HistoryEntry> {
    private final int age;

    public LastPageReadTask(int age) {
        this.age = age;
    }

    @Nullable @Override public HistoryEntry performTask() throws Throwable {
        List<Tab> tabList = Prefs.getTabs();
        if (age < tabList.size()) {
            List<PageBackStackItem> items = tabList.get(tabList.size() - age - 1).getBackStack();
            if (!items.isEmpty()) {
                return items.get(items.size() - 1).getHistoryEntry();
            }
        }
        return null;
    }
}

package org.wikipedia.page.tabs;

import org.wikipedia.page.PageBackStackItem;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;



public class Tab {
    @NonNull private final List<PageBackStackItem> backStack = new ArrayList<>();

    @NonNull
    public List<PageBackStackItem> getBackStack() {
        return backStack;
    }
}

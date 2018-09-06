package org.wikipedia.page.tabs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

public class Tab extends BaseModel {
    @NonNull private final List<PageBackStackItem> backStack = new ArrayList<>();

    @NonNull
    public List<PageBackStackItem> getBackStack() {
        return backStack;
    }

    @Nullable
    public PageTitle getTopMostTitle() {
        return backStack.isEmpty() ? null : backStack.get(backStack.size() - 1).getTitle();
    }
}

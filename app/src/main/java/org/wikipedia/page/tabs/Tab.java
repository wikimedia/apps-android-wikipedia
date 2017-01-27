package org.wikipedia.page.tabs;

import android.support.annotation.NonNull;

import org.wikipedia.model.BaseModel;
import org.wikipedia.page.PageBackStackItem;

import java.util.ArrayList;
import java.util.List;

public class Tab extends BaseModel {
    @NonNull private final List<PageBackStackItem> backStack = new ArrayList<>();

    @NonNull
    public List<PageBackStackItem> getBackStack() {
        return backStack;
    }
}

package org.wikipedia.page.tabs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.model.BaseModel;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

public class Tab extends BaseModel {
    @NonNull private final List<PageBackStackItem> backStack = new ArrayList<>();
    private int backStackPosition = -1;

    @NonNull
    public List<PageBackStackItem> getBackStack() {
        return backStack;
    }

    @Nullable
    public PageTitle getBackStackPositionTitle() {
        return backStack.isEmpty() ? null : backStack.get(getBackStackPosition()).getTitle();
    }

    public int getBackStackPosition() {
        if (backStackPosition < 0) {
            backStackPosition = backStack.size() - 1;
        }
        return backStackPosition;
    }

    public boolean canGoBack() {
        return getBackStackPosition() > 0;
    }

    public boolean canGoForward() {
        return getBackStackPosition() < backStack.size() - 1;
    }

    public void moveForward() {
        if (getBackStackPosition() < getBackStack().size() - 1) {
            backStackPosition++;
        }
    }

    public void moveBack() {
        if (getBackStackPosition() > 0) {
            backStackPosition--;
        }
    }

    public void pushBackStackItem(@NonNull PageBackStackItem item) {
        // remove all backstack items past the current position
        while (backStack.size() > getBackStackPosition() + 1) {
            backStack.remove(getBackStackPosition() + 1);
        }
        backStack.add(item);
        backStackPosition = backStack.size() - 1;
    }

    public void clearBackstack() {
        backStack.clear();
        backStackPosition = -1;
    }

    public void squashBackstack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        backStack.clear();
        backStack.add(item);
        backStackPosition = 0;
    }
}

package org.wikipedia.page;

import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

import org.wikipedia.views.TabCountsView;

public class PageToolbarHideHandler extends ViewHideHandler {

    PageToolbarHideHandler(@NonNull View hideableView, @NonNull TabCountsView tabsButton) {
        super(hideableView, null, Gravity.TOP);
        tabsButton.updateTabCount();
    }

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {
    }
}

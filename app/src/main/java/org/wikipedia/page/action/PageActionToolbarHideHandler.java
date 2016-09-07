package org.wikipedia.page.action;

import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.view.Gravity;

import org.wikipedia.page.ViewHideHandler;

public class PageActionToolbarHideHandler extends ViewHideHandler {
    public PageActionToolbarHideHandler(@NonNull TabLayout pageActions) {
        super(pageActions, Gravity.BOTTOM);
    }

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {
    }
}

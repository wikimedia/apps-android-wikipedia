package org.wikipedia.page.action;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;

import org.wikipedia.page.ViewHideHandler;

public class PageActionToolbarHideHandler extends ViewHideHandler {
    public PageActionToolbarHideHandler(@NonNull View pageActions, @Nullable View anchoredView) {
        super(pageActions, anchoredView, Gravity.BOTTOM);
    }

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {
    }
}

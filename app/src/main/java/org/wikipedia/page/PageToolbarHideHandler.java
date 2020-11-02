package org.wikipedia.page;

import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import org.wikipedia.views.TabCountsView;

public class PageToolbarHideHandler extends ViewHideHandler {

    PageToolbarHideHandler(@NonNull PageFragment pageFragment, @NonNull View hideableView,
                                  @NonNull Toolbar toolbar, @NonNull TabCountsView tabsButton) {
        super(hideableView, null, Gravity.TOP);
        tabsButton.updateTabCount();
    }

    /**
     * Whether to enable fading in/out of the search bar when near the top of the article.
     * @param enabled True to enable fading, false otherwise.
     */
    void setFadeEnabled(boolean enabled) {
        update();
    }

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {

    }
}

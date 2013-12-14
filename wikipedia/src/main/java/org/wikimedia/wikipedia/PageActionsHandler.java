package org.wikimedia.wikipedia;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikimedia.wikipedia.events.PageStateChangeEvent;
import org.wikimedia.wikipedia.events.SavePageEvent;
import org.wikimedia.wikipedia.savedpages.SavePageTask;

public class PageActionsHandler implements PopupMenu.OnMenuItemClickListener {
    private final PopupMenu menu;
    private final Bus bus;

    public PageActionsHandler(final Bus bus, final PopupMenu menu, final View trigger) {
        this.menu = menu;
        this.bus = bus;
        menu.getMenuInflater().inflate(R.menu.menu_page_actions, menu.getMenu());
        menu.setOnMenuItemClickListener(this);

        bus.register(this);

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.show();
            }
        });

    }

    @Subscribe
    public void onPageStateChange(PageStateChangeEvent event) {
        switch (event.getState()) {
            case PageViewFragment.STATE_NO_FETCH:
            case PageViewFragment.STATE_INITIAL_FETCH:
                menu.getMenu().findItem(R.id.menu_save_page).setEnabled(false);
                break;
            case PageViewFragment.STATE_COMPLETE_FETCH:
                menu.getMenu().findItem(R.id.menu_save_page).setEnabled(true);
                break;
            default:
                // How can this happen?!
                throw new RuntimeException("This can't happen");
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_page:
                bus.post(new SavePageEvent());
                break;
            default:
                throw new RuntimeException("Unexpected menu item clicked");
        }
        return false;
    }
}

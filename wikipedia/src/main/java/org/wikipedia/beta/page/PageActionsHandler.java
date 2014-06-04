package org.wikipedia.beta.page;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikipedia.beta.R;
import org.wikipedia.beta.events.BookmarkPageEvent;
import org.wikipedia.beta.events.PageStateChangeEvent;
import org.wikipedia.beta.events.SharePageEvent;
import org.wikipedia.beta.events.ShowOtherLanguagesEvent;

public class PageActionsHandler implements PopupMenu.OnMenuItemClickListener {
    private final PopupMenu menu;
    private final Bus bus;
    private final DrawerLayout navDrawer;

    public PageActionsHandler(final Bus bus, final PopupMenu menu, final View trigger, final DrawerLayout navDrawer) {
        this.menu = menu;
        this.bus = bus;
        this.navDrawer = navDrawer;
        menu.getMenuInflater().inflate(R.menu.menu_page_actions, menu.getMenu());
        menu.setOnMenuItemClickListener(this);

        bus.register(this);

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navDrawer.isDrawerOpen(Gravity.START)) {
                    navDrawer.closeDrawer(Gravity.START);
                }
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
                menu.getMenu().findItem(R.id.menu_share_page).setEnabled(false);
                menu.getMenu().findItem(R.id.menu_other_languages).setEnabled(false);
                break;
            case PageViewFragment.STATE_COMPLETE_FETCH:
                menu.getMenu().findItem(R.id.menu_save_page).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_share_page).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_other_languages).setEnabled(true);
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
                bus.post(new BookmarkPageEvent());
                break;
            case R.id.menu_share_page:
                bus.post(new SharePageEvent());
                break;
            case R.id.menu_other_languages:
                bus.post(new ShowOtherLanguagesEvent());
                break;
            default:
                throw new RuntimeException("Unexpected menu item clicked");
        }
        return false;
    }
}

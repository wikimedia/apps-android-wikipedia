package org.wikipedia.page;

import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.*;

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

    public void onDestroy() {
        bus.unregister(this);
    }

    @Subscribe
    public void onOverflowMenuChange(OverflowMenuUpdateEvent event) {
        switch (event.getState()) {
            case PageViewFragmentInternal.STATE_NO_FETCH:
            case PageViewFragmentInternal.STATE_INITIAL_FETCH:
                menu.getMenu().findItem(R.id.menu_save_page).setEnabled(false);
                menu.getMenu().findItem(R.id.menu_share_page).setEnabled(false);
                menu.getMenu().findItem(R.id.menu_other_languages).setEnabled(false);
                menu.getMenu().findItem(R.id.menu_find_in_page).setEnabled(false);
                menu.getMenu().findItem(R.id.menu_themechooser).setEnabled(false);
                break;
            case PageViewFragmentInternal.STATE_COMPLETE_FETCH:
                menu.getMenu().findItem(R.id.menu_save_page).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_share_page).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_other_languages).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_find_in_page).setEnabled(true);
                menu.getMenu().findItem(R.id.menu_themechooser).setEnabled(true);
                if (event.getSubstate() == PageViewFragmentInternal.SUBSTATE_PAGE_SAVED) {
                    menu.getMenu().findItem(R.id.menu_save_page).setEnabled(false);
                    menu.getMenu().findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_page_saved));
                } else if (event.getSubstate() == PageViewFragmentInternal.SUBSTATE_SAVED_PAGE_LOADED) {
                    menu.getMenu().findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_refresh_saved_page));
                } else {
                    menu.getMenu().findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_save_page));
                }
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
            case R.id.menu_share_page:
                bus.post(new SharePageEvent());
                break;
            case R.id.menu_other_languages:
                bus.post(new ShowOtherLanguagesEvent());
                break;
            case R.id.menu_find_in_page:
                bus.post(new FindInPageEvent());
                break;
            case R.id.menu_themechooser:
                bus.post(new ShowThemeChooserEvent());
                break;
            default:
                throw new RuntimeException("Unexpected menu item clicked");
        }
        return false;
    }
}

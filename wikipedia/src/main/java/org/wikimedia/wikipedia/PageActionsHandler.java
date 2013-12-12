package org.wikimedia.wikipedia;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.squareup.otto.Bus;
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

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.show();
            }
        });

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

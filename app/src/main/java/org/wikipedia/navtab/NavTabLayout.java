package org.wikipedia.navtab;

import android.content.Context;
import android.support.design.widget.BottomNavigationView;
import android.util.AttributeSet;
import android.view.Menu;

public class NavTabLayout extends BottomNavigationView {

    public NavTabLayout(Context context) {
        super(context);
        setTabViews();
    }

    public NavTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTabViews();
    }

    public NavTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTabViews();
    }

    private void setTabViews() {
        for (int i = 0; i < NavTab.size(); i++) {
            NavTab navTab = NavTab.of(i);
            getMenu().add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon());
        }
    }
}

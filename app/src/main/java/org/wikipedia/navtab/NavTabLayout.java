package org.wikipedia.navtab;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.wikipedia.auth.AccountUtil;

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

    public void setTabViews() {
        for (int i = 0; i < NavTab.size(); i++) {
            NavTab navTab = NavTab.of(i);
            if (AccountUtil.isLoggedIn()) {
                getMenu().add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon());
            } else {
                if (!(NavTab.SUGGESTED_EDITS == navTab)) {
                    getMenu().add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon());
                }
            }
        }
    }

    public void clear() {
        getMenu().clear();
    }
}

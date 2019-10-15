package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu

import com.google.android.material.bottomnavigation.BottomNavigationView

import org.wikipedia.auth.AccountUtil

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        setTabViews()
    }

    fun setTabViews() {
        menu.clear()
        for (i in 0 until NavTab.size()) {
            val navTab = NavTab.of(i)
            if (!AccountUtil.isLoggedIn() && NavTab.SUGGESTED_EDITS === navTab) {
                continue
            }
            menu.add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon())
        }
    }
}

package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        for (i in 0 until NavTab.size()) {
            val navTab = NavTab.of(i)
            menu.add(Menu.NONE, navTab.id(), i, navTab.text()).setIcon(navTab.icon())
        }
    }
}

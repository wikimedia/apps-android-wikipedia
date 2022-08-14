package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        NavTab.SET.forEachIndexed { index, navTab ->
            menu.add(Menu.NONE, navTab.id(), index, navTab.text()).setIcon(navTab.icon())
        }
    }
}

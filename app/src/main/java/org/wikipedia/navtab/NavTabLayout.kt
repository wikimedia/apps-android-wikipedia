package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        NavTab.entries.forEachIndexed { index, tab ->
            menu.add(Menu.NONE, tab.id, index, tab.text).setIcon(tab.icon)
        }
    }
}

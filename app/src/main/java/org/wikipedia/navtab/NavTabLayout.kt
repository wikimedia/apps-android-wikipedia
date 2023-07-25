package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        for (i in 0 until NavTab.size()) {
            val navTab = NavTab.of(i)
            menu.add(Menu.NONE, navTab.id, i, navTab.text).setIcon(navTab.icon)
        }
    }

    fun updateMaxLines(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is ViewGroup) {
                updateMaxLines(it)
            } else if (it is TextView) {
                it.maxLines = 2
            }
        }
    }
}

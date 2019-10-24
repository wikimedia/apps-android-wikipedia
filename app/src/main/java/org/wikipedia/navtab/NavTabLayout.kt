package org.wikipedia.navtab

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.google.android.material.bottomnavigation.BottomNavigationView
import org.wikipedia.R

import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.DimenUtil

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
        fixTextStyle()
    }

    private fun fixTextStyle() {
        if (childCount > 0) {
            val menuView = getChildAt(0)
            if ((menuView as ViewGroup).childCount > 0) {
                for (i in 0..menuView.childCount) {
                    val menuItemView = menuView.getChildAt(i)
                    if (menuItemView != null) {
                        val labelView = menuItemView.findViewById<TextView>(R.id.largeLabel)
                        labelView.setSingleLine(false)
                        labelView.maxLines = 2
                        labelView.ellipsize = TextUtils.TruncateAt.END
                        labelView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }
                }
            }
        }
    }
}

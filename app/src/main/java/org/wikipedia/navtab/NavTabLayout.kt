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

    fun fixTextStyle() {
        // Todo: update later https://github.com/material-components/material-components-android/issues/139
        if (childCount > 0) {
            val menuView = getChildAt(0)
            if ((menuView as ViewGroup).childCount > 0) {
                for (i in 0 until menuView.childCount) {
                    val menuChildView = menuView.getChildAt(i)
                    if (menuChildView != null) {
                        val labelView = menuChildView.findViewById<TextView>(R.id.largeLabel)
                        labelView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        labelView.setSingleLine(false)
                        labelView.maxLines = 2
                        labelView.ellipsize = TextUtils.TruncateAt.END
                    }
                }
            }
        }
    }

}

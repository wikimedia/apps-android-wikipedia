package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

open class ConfigurableTabLayout constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private fun enableTab(index: Int) {
        val tab = getChildAt(index)
        if (tab != null) {
            setEnabled(tab, true)
        }
    }

    fun disableTab(index: Int) {
        val tab = getChildAt(index)
        if (tab != null) {
            setEnabled(tab, false)
        }
    }

    fun enableAllTabs() {
        for (i in 0 until childCount) {
            enableTab(i)
        }
    }

    fun isEnabled(tab: View): Boolean {
        return !isDisabled(tab)
    }

    private fun isDisabled(tab: View): Boolean {
        return tab.tag != null && tab.tag is DisabledTag
    }

    private fun setEnabled(tab: View, enabled: Boolean) {
        tab.tag = if (enabled) null else DisabledTag()
    }

    private class DisabledTag
}

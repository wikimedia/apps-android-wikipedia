package org.wikipedia.history

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import org.wikipedia.views.SearchActionProvider

abstract class SearchActionModeCallback : ActionMode.Callback {

    protected abstract fun getSearchHintString(): String
    protected abstract fun onQueryChange(s: String)
    protected abstract fun getParentContext(): Context

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.tag = ACTION_MODE_TAG
        val menuItem = menu.add(getSearchHintString())
        // Manually setup a action provider to be able to adjust the left margin of the search field.
        MenuItemCompat.setActionProvider(menuItem, SearchActionProvider(getParentContext(), getSearchHintString()) { onQueryChange(it) })
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
    }

    companion object {
        const val ACTION_MODE_TAG: String = "searchActionMode"

        fun matches(mode: ActionMode?): Boolean {
            return ACTION_MODE_TAG == mode?.tag
        }
    }
}

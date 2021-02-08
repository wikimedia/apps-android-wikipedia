package org.wikipedia.views

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import org.wikipedia.R

abstract class MultiSelectActionModeCallback : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.tag = ACTION_MODE_TAG
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_delete_selected -> {
                onDeleteSelected()
                return true
            }
            else -> { }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {}

    protected abstract fun onDeleteSelected()

    companion object {
        private const val ACTION_MODE_TAG = "multiSelectActionMode"
        @JvmStatic
        fun isTagType(mode: ActionMode?): Boolean {
            return mode != null && ACTION_MODE_TAG == mode.tag
        }
    }
}

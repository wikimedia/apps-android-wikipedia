package org.wikipedia.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import org.wikipedia.R
import org.wikipedia.history.SearchActionModeCallback

class DeveloperSettingsFragment : PreferenceLoaderFragment(), MenuProvider {

    private val searchActionModeCallback = DevPreferencesSearchCallback()
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun loadPreferences() {
        DeveloperSettingsPreferenceLoader(this).loadPreferences()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_developer_settings, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_search -> {
                (requireActivity() as AppCompatActivity).startSupportActionMode(searchActionModeCallback)
                true
            }
            else -> false
        }
    }

    private inner class DevPreferencesSearchCallback : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            DeveloperSettingsPreferenceLoader(this@DeveloperSettingsFragment).filterPreferences(s.trim())
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            DeveloperSettingsPreferenceLoader(this@DeveloperSettingsFragment).filterPreferences()
        }

        override fun getSearchHintString(): String {
            return requireContext().resources.getString(R.string.preferences_developer_search_hint)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    companion object {
        fun newInstance(): DeveloperSettingsFragment {
            return DeveloperSettingsFragment()
        }
    }
}

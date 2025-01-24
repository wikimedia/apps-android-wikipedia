package org.wikipedia.settings

import android.content.Context
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import org.wikipedia.R
import org.wikipedia.history.SearchActionModeCallback

class DeveloperSettingsFragment : PreferenceLoaderFragment() {

    private val searchActionModeCallback = DevPreferencesSearchCallback()
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun loadPreferences() {
        DeveloperSettingsPreferenceLoader(this).loadPreferences()
    }

    fun startSearchActionMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(searchActionModeCallback)
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

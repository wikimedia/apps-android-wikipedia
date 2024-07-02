package org.wikipedia.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent
import org.wikipedia.events.ReadingListsEnabledStatusEvent
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent
import org.wikipedia.settings.DeveloperSettingsActivity.Companion.newIntent

class SettingsFragment : PreferenceLoaderFragment(), MenuProvider {
    private lateinit var preferenceLoader: SettingsPreferenceLoader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is ReadingListsEnabledStatusEvent -> {
                            setReadingListSyncPref(true)
                        }
                        is ReadingListsNoLongerSyncedEvent -> {
                            setReadingListSyncPref(false)
                        }
                        is ReadingListsEnableSyncStatusEvent -> {
                            setReadingListSyncPref(Prefs.isReadingListSyncEnabled)
                        }
                    }
                }
            }
        }

        // TODO: Kick off a sync of reading lists, which will call back to us whether lists
        // are enabled or not. (Not sure if this is necessary yet.)
    }

    override fun loadPreferences() {
        preferenceLoader = SettingsPreferenceLoader(this)
        preferenceLoader.loadPreferences()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.decorView.post {
            if (!isAdded) {
                return@post
            }
            preferenceLoader.updateSyncReadingListsPrefSummary()
            preferenceLoader.updateLanguagePrefSummary()
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        prepareDeveloperSettingsMenuItem(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.developer_settings -> {
                launchDeveloperSettingsActivity()
                true
            }
            else -> false
        }
    }

    private fun launchDeveloperSettingsActivity() {
        startActivity(newIntent(requireActivity()))
    }

    private fun prepareDeveloperSettingsMenuItem(menu: Menu) {
        menu.findItem(R.id.developer_settings).isVisible = Prefs.isShowDeveloperSettingsEnabled
    }

    private fun setReadingListSyncPref(checked: Boolean) {
        (preferenceLoader.findPreference(R.string.preference_key_sync_reading_lists) as SwitchPreferenceCompat).isChecked = checked
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}

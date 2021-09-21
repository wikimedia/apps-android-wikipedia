package org.wikipedia.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.SwitchPreferenceCompat
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent
import org.wikipedia.events.ReadingListsEnabledStatusEvent
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent
import org.wikipedia.settings.DeveloperSettingsActivity.Companion.newIntent

class SettingsFragment : PreferenceLoaderFragment() {
    private lateinit var preferenceLoader: SettingsPreferenceLoader
    private val disposables = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disposables.add(WikipediaApp.instance.bus.subscribe(EventBusConsumer()))

        // TODO: Kick off a sync of reading lists, which will call back to us whether lists
        // are enabled or not. (Not sure if this is necessary yet.)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        prepareDeveloperSettingsMenuItem(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.developer_settings -> {
                launchDeveloperSettingsActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
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

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}

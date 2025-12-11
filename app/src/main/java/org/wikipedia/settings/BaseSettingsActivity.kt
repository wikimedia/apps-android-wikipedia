package org.wikipedia.settings

import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity

abstract class BaseSettingsActivity<T : PreferenceLoaderFragment> : SingleFragmentActivity<T>() {
    abstract val title: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(title)
    }

    override fun inflateAndSetContentView() {
        setContentView(R.layout.activity_settings_base)
    }
}

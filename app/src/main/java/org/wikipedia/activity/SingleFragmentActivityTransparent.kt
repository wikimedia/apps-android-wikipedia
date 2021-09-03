package org.wikipedia.activity

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.wikipedia.R

/**
 * Boilerplate for a FragmentActivity containing a single stack of
 * Fragments, with a transparent background.
 *
 * Set a theme on the activity in AndroidManifest.xml to specify a background tint.
 */
abstract class SingleFragmentActivityTransparent<T : Fragment> : SingleFragmentActivity<T>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requireViewById<View>(this, R.id.fragment_container).background = null
    }

    override fun setTheme() {
        setTheme(R.style.ThemeDark_Translucent)
    }
}

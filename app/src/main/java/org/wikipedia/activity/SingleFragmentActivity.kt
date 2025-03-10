package org.wikipedia.activity

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import org.wikipedia.R
import org.wikipedia.util.DeviceUtil

/**
 * Boilerplate for a FragmentActivity containing a single stack of Fragments.
 */
abstract class SingleFragmentActivity<T : Fragment> : BaseActivity() {
    lateinit var fragment: T

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceUtil.assertAppContext(this)) {
            return
        }

        inflateAndSetContentView()

        val currentFragment: T? = supportFragmentManager.findFragmentById(R.id.fragment_container) as T?
        if (currentFragment != null) {
            fragment = currentFragment
        } else {
            fragment = createFragment()
            supportFragmentManager.commit { add(R.id.fragment_container, fragment) }
        }
    }

    protected abstract fun createFragment(): T

    protected open fun inflateAndSetContentView() {
        setContentView(R.layout.activity_single_fragment)
    }

    fun disableFitsSystemWindows() {
        findViewById<FrameLayout>(R.id.fragment_container).fitsSystemWindows = false
    }
}

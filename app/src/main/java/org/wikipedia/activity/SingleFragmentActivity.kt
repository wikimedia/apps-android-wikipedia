package org.wikipedia.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import org.wikipedia.R

/**
 * Boilerplate for a FragmentActivity containing a single stack of Fragments.
 */
abstract class SingleFragmentActivity<T : Fragment> : BaseActivity() {
    lateinit var fragment: T

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

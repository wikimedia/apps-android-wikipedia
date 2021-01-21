package org.wikipedia.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import org.wikipedia.R

/**
 * Boilerplate for a FragmentActivity containing a single stack of Fragments.
 */
abstract class SingleFragmentActivity<T : Fragment> : BaseActivity() {
    lateinit var fragment: T

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        fragment = createFragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment).commit()
    }

    protected abstract fun createFragment(): T

    /** @return The resource layout to inflate which must contain a [android.view.ViewGroup]
     * whose ID is [.getContainerId].
     */
    @get:LayoutRes
    protected open val layout: Int
        get() = R.layout.activity_single_fragment
}

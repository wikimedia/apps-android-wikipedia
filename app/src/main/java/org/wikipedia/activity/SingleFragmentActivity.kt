package org.wikipedia.activity

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import org.wikipedia.R


/**
 * Boilerplate for a FragmentActivity containing a single stack of Fragments.
 */
abstract class SingleFragmentActivity<T : Fragment> : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        if (getFragment() == null) {
            supportFragmentManager.beginTransaction().add(containerId, createFragment()).commit()
        }
    }

    protected abstract fun createFragment(): T

    /** @return The Fragment added to the stack.
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun getFragment(): T? {
        return supportFragmentManager.findFragmentById(containerId) as T?
    }

    /** @return The resource layout to inflate which must contain a [android.view.ViewGroup]
     * whose ID is [.getContainerId].
     */
    @get:LayoutRes
    protected open val layout: Int
        get() = R.layout.activity_single_fragment

    /** @return The resource identifier for the Fragment container.
     */
    @get:IdRes
    protected open val containerId: Int
        get() = R.id.fragment_container
}

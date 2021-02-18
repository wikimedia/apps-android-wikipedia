package org.wikipedia

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class viewLifeCycleDelegate {
    fun <T> Fragment.viewLifecycle(bindUntilEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY): ReadWriteProperty<Fragment, T> =
            object: ReadWriteProperty<Fragment, T>, LifecycleObserver {

                // A backing property to hold our value
                private var binding: T? = null

                private var viewLifecycleOwner: LifecycleOwner? = null

                init {
                    // Observe the View Lifecycle of the Fragment
                    this@viewLifecycle
                            .viewLifecycleOwnerLiveData
                            .observe(this@viewLifecycle, Observer { newLifecycleOwner ->
                                viewLifecycleOwner
                                        ?.lifecycle
                                        ?.removeObserver(this)

                                viewLifecycleOwner = newLifecycleOwner.also {
                                    it.lifecycle.addObserver(this)
                                }
                            })
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
                fun onDestroy(event: Lifecycle.Event) {
                    if (event == bindUntilEvent) {
                        // Clear out backing property just before onDestroyView
                        binding = null
                    }
                }

                override fun getValue(
                        thisRef: Fragment,
                        property: KProperty<*>
                ): T {
                    // Return the backing property if it's set
                    return this.binding!!
                }
                override fun setValue(
                        thisRef: Fragment,
                        property: KProperty<*>,
                        value: T
                ) {
                    // Set the backing property
                    this.binding = value
                }

            }

}

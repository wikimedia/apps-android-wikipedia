package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable

abstract class SuggestedEditsItemFragment : Fragment() {
    val disposables = CompositeDisposable()
    var pagerPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt("pagerPosition", -1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pagerPosition", pagerPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    fun parent(): SuggestedEditsCardsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as SuggestedEditsCardsFragment
    }

    open fun publish() {
    }
}
